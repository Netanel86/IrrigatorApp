from __future__ import annotations
from collections import namedtuple
import logging
from typing import Any, Callable, Dict, List, Tuple
from datetime import datetime
from enum import Enum
from model import AnalogSensor, EPModule, SensorType
from infra import DictParseable
from data.firestore import FirestoreConnection, OrderBy, Where
from constants import Local, Remote
from extensions import reverse_dict, is_empty
from data.sqlite import (
    COL_ROWID,
    PARSE,
    QueryBuilder,
    SQLiteConnection,
)


MODULE_TO_LOCAL_MAP = {
    EPModule.Props().ID: Local.Modules.ColName.ID,
    EPModule.Props().MAC_ID: Local.Modules.ColName.MAC_ID,
    EPModule.Props().DESCRIPTION: Local.Modules.ColName.DESCRIPTION,
    EPModule.Props().MAX_DURATION: Local.Modules.ColName.MAX_DURATION,
    EPModule.Props().DURATION: Local.Modules.ColName.DURATION,
    EPModule.Props().ON_TIME: Local.Modules.ColName.ON_TIME,
    EPModule.Props().PORT: Local.Modules.ColName.PORT,
    EPModule.Props().TIMEOUT: Local.Modules.ColName.TIMEOUT,
}
"""A Mapping of `ModelLib.EPModule` properties to local database 'Module' table columns (key=prop_name, val=col_name)."""

MODULE_FROM_LOCAL_MAP = reverse_dict(MODULE_TO_LOCAL_MAP)
"""A Mapping of local database 'Module' table columns to `ModelLib.EPModule` properties (key=col_name, val=prop_name)."""

MODULE_TO_REMOTE_MAP = {
    EPModule.Props().MAC_ID: Remote.Modules.FieldName.IP,
    EPModule.Props().DESCRIPTION: Remote.Modules.FieldName.DESCRIPTION,
    EPModule.Props().MAX_DURATION: Remote.Modules.FieldName.MAX_DURATION,
    EPModule.Props().DURATION: Remote.Modules.FieldName.DURATION,
    EPModule.Props().ON_TIME: Remote.Modules.FieldName.ON_TIME,
}
"""A Mapping of `ModelLib.EPModule` properties to remote database 'Modules' collection fields (key=prop_name, val=field_name)."""

MODULE_FROM_REMOTE_MAP = reverse_dict(MODULE_TO_REMOTE_MAP)
"""A Mapping of remote database 'Modules' collection fields to `ModelLib.EPModule` properties (key=field_name, val=prop_name)."""

MODULE_FIELDS = tuple(MODULE_TO_REMOTE_MAP.keys())

SENSOR_TO_LOCAL_MAP = {
    AnalogSensor.Props().ID: Local.Sensors.ColName.ID,
    AnalogSensor.Props().TYPE: Local.Sensors.ColName.TYPE,
    AnalogSensor.Props().MIN_VALUE: Local.Sensors.ColName.MIN_VAL,
    AnalogSensor.Props().MAX_VALUE: Local.Sensors.ColName.MAX_VAL,
    AnalogSensor.Props().CURRENT_VAL: Local.Sensors.ColName.CURR_VAL,
}

SENSOR_FROM_LOCAL_MAP = reverse_dict(SENSOR_TO_LOCAL_MAP)

SENSOR_COLUMNS = tuple(SENSOR_FROM_LOCAL_MAP.keys())

SENSOR_TO_REMOTE_MAP = {
    AnalogSensor.Props().TYPE: Remote.Sensors.FieldName.TYPE,
    AnalogSensor.Props().MIN_VALUE: Remote.Sensors.FieldName.MIN_VAL,
    AnalogSensor.Props().MAX_VALUE: Remote.Sensors.FieldName.MAX_VAL,
    AnalogSensor.Props().CURRENT_VAL: Remote.Sensors.FieldName.CURR_VAL,
}

SENSOR_FROM_REMOTE_MAP = reverse_dict(SENSOR_TO_REMOTE_MAP)

SENSOR_FIELDS = tuple(SENSOR_TO_REMOTE_MAP.keys())


class Repository(object):
    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
        self._modules: Dict[str, EPModule] = {}
        self.__init_local()
        self.__init_remote()

    def __init_local(self):
        self._local = SQLiteConnection()
        self._local.create(Local.System.TABLE_NAME, Local.System.TYPE_MAP)
        self._local.create(Local.Modules.TABLE_NAME, Local.Modules.TYPE_MAP)
        self._local.create(Local.Sensors.TABLE_NAME, Local.Sensors.TYPE_MAP)

    def __init_remote(self):
        self.__remote = FirestoreConnection()
        self.SYS_ID = self.__get_system_id()
        self.__init_paths(self.SYS_ID)

    def __init_paths(self, system_id: str):
        if system_id is None:
            raise ValueError(
                "system_id: value can't be None, call get_system_id() before initializing paths"
            )

        self.PATH_SYSTEM = "{0}/{1}".format(Remote.Systems.COLL_NAME, system_id)
        self.PATH_COMMANDS = "{0}/{1}".format(
            self.PATH_SYSTEM, Remote.Commands.COLL_NAME
        )
        self.PATH_MODULES = "{0}/{1}".format(self.PATH_SYSTEM, Remote.Modules.COLL_NAME)

        self.PATH_SUBCOL_SENSORS = {}
        if self._local is not None:
            module_ids = self._local.select(
                Local.Modules.TABLE_NAME, (Local.Modules.ColName.ID,)
            ).execute(PARSE.DICT)
            for id_dict in module_ids:
                self.PATH_SUBCOL_SENSORS[
                    id_dict[Local.Modules.ColName.ID]
                ] = "{0}/{1}/{2}".format(
                    self.PATH_MODULES,
                    id_dict[Local.Modules.ColName.ID],
                    Remote.Sensors.COLL_NAME,
                )

    def __get_system_id(self) -> str:
        """Retrievs the system id.

        retrieve the system id from the local db, if one doesn't exist,
        request a new system reference id from the remote db.

        Returns:
            A new or existing system id.
        """
        sys_ans = (
            self._local.select(Local.System.TABLE_NAME, (Local.System.ColName.ID,))
            .where((COL_ROWID,), (1,))
            .execute(PARSE.DICT)
        )

        if sys_ans == False or len(sys_ans) == 0:
            system_id = self.__remote.document_ref(Remote.Systems.COLL_NAME)
            self._local.insert(
                Local.System.TABLE_NAME, {Local.System.ColName.ID: system_id}
            )
        else:
            system_id = sys_ans[0][Local.System.ColName.ID]

        return system_id

    def __assign_ids(self, objects: List[DictParseable], ids_list: List[str]):
        for idx, obj in enumerate(objects):
            obj.id = ids_list[idx]

    def add_module(self, module: EPModule):
        # module.id = self.__remote.add_document(
        #     self.PATH_MODULES, module.to_dict(to_map=MODULE_TO_REMOTE_MAP)
        # )

        # self._local.insert(
        #     Local.Modules.TABLE_NAME,
        #     module.to_dict(to_map=MODULE_TO_LOCAL_MAP),
        # )

        # self.add_sensors(module.id, module._sensors)

        self._modules[module.mac_id] = module
        self.__init_on_change_callbacks(module)

        return module.id if not is_empty(module.id) else None

    def add_sensors(self, module_id: str, sensors: List[AnalogSensor]):
        if not is_empty(sensors):
            new_sensors = [sensor for sensor in sensors if is_empty(sensor.id)]

            sens_rem_dicts = [
                sensor.to_dict(to_map=SENSOR_TO_REMOTE_MAP) for sensor in new_sensors
            ]

            sensors_path = self.PATH_SUBCOL_SENSORS.get(module_id, None)
            if sensors_path is None:
                sensors_path = self.PATH_SUBCOL_SENSORS[module_id] = "{}/{}/{}".format(
                    self.PATH_MODULES, module_id, Remote.Sensors.COLL_NAME
                )

            sens_ids = self.__remote.add_documents(sensors_path, sens_rem_dicts)
            self.__assign_ids(new_sensors, sens_ids)

            sens_loc_dicts = []
            for sensor in new_sensors:
                sens_dict = sensor.to_dict(to_map=SENSOR_TO_LOCAL_MAP)
                sens_dict[Local.Sensors.ColName.MODULE_ID] = module_id
                sens_loc_dicts.append(sens_dict)

            return self._local.insert(Local.Sensors.TABLE_NAME, sens_loc_dicts)

    def add_many_sensors(
        self,
        module_ids: List[str],
        sensor_lists: List[List[AnalogSensor]],
    ):
        new_sensor_lists: List[List[AnalogSensor]] = []

        for sensors in sensor_lists:
            new_sensor_lists.append(
                [sensor for sensor in sensors if is_empty(sensor.id)]
                if sensors
                else None
            )

        for idx, sensors in enumerate(new_sensor_lists):
            sens_rem_dicts = []
            if sensors:
                sens_rem_dicts = [
                    sensor.to_dict(to_map=SENSOR_TO_REMOTE_MAP) for sensor in sensors
                ]

                sensors_path = self.PATH_SUBCOL_SENSORS.get(module_ids[idx], None)
                if sensors_path is None:
                    sensors_path = self.PATH_SUBCOL_SENSORS[
                        module_ids[idx]
                    ] = "{}/{}/{}".format(
                        self.PATH_MODULES, module_ids[idx], Remote.Sensors.COLL_NAME
                    )

                sens_ids = self.__remote.add_documents(sensors_path, sens_rem_dicts)
                self.__assign_ids(sensors, sens_ids)

        sens_loc_dicts = []
        for idx, sensors in enumerate(new_sensor_lists):
            if sensors:
                for sensor in sensors:
                    sens_dict = sensor.to_dict(to_map=SENSOR_TO_LOCAL_MAP)
                    sens_dict[Local.Sensors.ColName.MODULE_ID] = module_ids[idx]
                    sens_loc_dicts.append(sens_dict)

        return self._local.insert(Local.Sensors.TABLE_NAME, sens_loc_dicts)

    def add_modules(self, modules: List[EPModule]) -> int | None:
        remote_dicts = []
        for module in modules:
            remote_dicts.append(module.to_dict(to_map=MODULE_TO_REMOTE_MAP))

        doc_ids = self.__remote.add_documents(self.PATH_MODULES, remote_dicts)

        local_dicts = []
        module_ids = []
        all_sensors: List[List[SensorType]] = []
        for idx, module in enumerate(modules):
            module.id = doc_ids[idx]
            module_ids.append(module.id)
            all_sensors.append(
                module._sensors if not is_empty(module._sensors) else None
            )
            local_dicts.append(module.to_dict(to_map=MODULE_TO_LOCAL_MAP))
            self._modules[module.mac_id] = module

        insert_count = self._local.insert(Local.Modules.TABLE_NAME, local_dicts)

        self.add_many_sensors(module_ids, all_sensors)
        return doc_ids if len(doc_ids) == insert_count else None

    def get_modules(self) -> Dict[str, EPModule]:
        if is_empty(self._modules):
            # self.__modules = (
            #     self.__local.select(Local.Modules.TABLE_NAME)
            #     .join(
            #         Local.Sensors.TABLE_NAME,
            #         SENSOR_COLUMNS,
            #         QueryBuilder.JOIN.LEFT,
            #         src_col=Local.Modules.ColName.ID,
            #         target_col=Local.Sensors.ColName.MODULE_ID,
            #     )
            #     .orderby((Local.Modules.ColName.MAC_ID,))
            #     .execute(self.__parse_modules)
            # )
            module = EPModule("1:2:3:4:5")
            module.description = "callback tester"
            module.add_sensors(AnalogSensor(SensorType.EC))
            self._modules[module.mac_id] = module

            for module in self._modules.values():
                self.__init_on_change_callbacks(module)

        return self._modules

    def __init_on_change_callbacks(self, module: EPModule):
        module.register_callback(self.__on_module_change)
        for sensor in module.sensors:
            sensor.register_callback(
                lambda sen, prop, old, new: self.__on_sensor_change(
                    module.id, sen, prop, old, new
                )
            )

    def __on_module_change(self, module: EPModule, property: str, old_val, new_val):
        method_sig = self.__on_module_change.__name__.removeprefix("__")
        if property == EPModule.Props().SENSORS:
            # self.add_sensors(module.id, new_val)

            self.logger.info(
                f"{method_sig}> add {len(new_val) if isinstance(new_val, list) else 1} new sensors."
            )
        else:
            # updated = self.update_module(
            #     module,
            #     [
            #         property,
            #     ],
            #     remote=True,
            # )
            # if not updated:
            #     method_sig = (
            #         f"{self.__class__.__name__}.{self.__on_module_change.__name__}()"
            #     )
            #     self.__logger.warn(
            #         f"{method_sig}: Failed to update property '{property}' in module [{module.mac_id}]"
            #     )
            # else:
            self.logger.info(
                f"{method_sig}> update module property '{property}' from '{old_val}' to '{new_val}'"
            )

    def __on_sensor_change(
        self, module_id, sensor: AnalogSensor, property: str, old_val, new_val
    ):
        method_sig = self.__on_sensor_change.__name__.removeprefix("__")
        # updated = self.update_sensor(
        #     module_id,
        #     sensor,
        #     [
        #         property,
        #     ],
        #     remote=True,
        # )
        # if not updated:
        #     method_sig = (
        #         f"{self.__class__.__name__}.{self.__on_sensor_change.__name__}()"
        #     )
        #     self.__logger.warning(
        #         f"{method_sig}: Failed to update property '{property}' in sensor id [{sensor.id}], parent module id [{module_id}]"
        #     )
        # else:
        self.logger.info(
            f"{method_sig}> update sensor in module {module_id} property '{property}' from '{old_val}' to '{new_val}'"
        )

    def __parse_modules(self, col_data: Tuple[Tuple], values: List[Tuple]):
        mod_col_count = len(Local.Modules.COLUMNS)
        sen_col_last_idx = mod_col_count + len(Local.Sensors.COLUMNS)

        modules: Dict[str, EPModule] = {}
        module_cols = col_data[:mod_col_count]
        sensor_cols = col_data[mod_col_count:sen_col_last_idx]

        for tup in values:
            module_dict = self._local.merge_to_dict(module_cols, tup[:mod_col_count])
            sensor_dict = self._local.merge_to_dict(
                sensor_cols, tup[mod_col_count:sen_col_last_idx]
            )

            module_ip = module_dict[Local.Modules.ColName.MAC_ID]
            if module_ip not in modules.keys():
                modules[module_ip] = EPModule.from_dict(
                    module_dict, MODULE_FROM_LOCAL_MAP
                )

            if sensor_dict[Local.Sensors.ColName.ID] is not None:
                sensor = AnalogSensor.from_dict(sensor_dict, SENSOR_FROM_LOCAL_MAP)
                modules[module_ip].sensors.append(sensor)

        return modules

    def get_commands(self) -> List[Command]:
        cmnd_dicts = self.__remote.get_collection(
            self.PATH_COMMANDS, OrderBy(Command.PROP_TIME, OrderBy.DESCENDING)
        )
        return self.__remote.map_to_object(
            cmnd_dicts,
            Command.Props().ID,
            lambda dic: Command.from_dict(dic, COMMAND_FROM_REMOTE_MAP),
        )

    def update_module(self, module: EPModule, props: List[str] = None, **kwargs):
        local: bool = kwargs.get("local", True)
        remote: bool = kwargs.get("remote", False)

        if not local and not remote:
            raise ValueError(
                "in {}.{}(): both 'local' and 'remote' values are set to False, nothing will be updated".format(
                    self.__class__.__name__, self.update_module.__name__
                )
            )
        if local:
            loc_dict = (
                module.to_dict(props, MODULE_TO_LOCAL_MAP)
                if props
                else module.to_dict(to_map=MODULE_TO_LOCAL_MAP)
            )

            local_done = (
                self._local.update(Local.Modules.TABLE_NAME, loc_dict)
                .where((Local.Modules.ColName.ID,), (module.id,))
                .execute()
            )

        if remote:
            if props is not None:
                rem_props = tuple(
                    prop for prop in props if prop in Remote.Modules.FIELDS
                )
                remot_dict = module.to_dict(rem_props, MODULE_TO_REMOTE_MAP)
            else:
                remot_dict = module.to_dict(to_map=MODULE_TO_REMOTE_MAP)

            self.__remote.update_document(self.PATH_MODULES, module.id, remot_dict)

        return local_done

    def update_sensor(
        self, module_id: str, sensor: AnalogSensor, props: List[str] = None, **kwargs
    ):
        local: bool = kwargs.get("local", True)
        remote: bool = kwargs.get("remote", False)

        if local:
            loc_dict = (
                sensor.to_dict(props, SENSOR_TO_LOCAL_MAP)
                if props
                else sensor.to_dict(to_map=SENSOR_TO_LOCAL_MAP)
            )

            local_result = (
                self._local.update(Local.Sensors.TABLE_NAME, loc_dict)
                .where((Local.Sensors.ColName.ID,), (sensor.id,))
                .execute()
            )

        if remote:
            if props is not None:
                rem_props = tuple(prop for prop in props if prop in SENSOR_FIELDS)
                rem_dict = sensor.to_dict(rem_props, SENSOR_TO_REMOTE_MAP)
            else:
                rem_dict = sensor.to_dict(to_map=SENSOR_TO_REMOTE_MAP)

            self.__remote.update_document(
                self.PATH_SUBCOL_SENSORS[module_id],
                sensor.id,
                rem_dict,
            )

        return local_result

    def update_sensors(self, module: EPModule, props: List[str] = None, **kwargs):
        local: bool = kwargs.get("local", True)
        remote: bool = kwargs.get("remote", False)
        sensor_ids = [sensor.id for sensor in module._sensors]
        sensor_dicts: List[Dict[str, Any]] = None
        if local:
            sensor_dicts = [
                sensor.to_dict(props, SENSOR_TO_LOCAL_MAP)
                if props
                else sensor.to_dict(to_map=SENSOR_TO_LOCAL_MAP)
                for sensor in module._sensors
            ]

            local_result = (
                self._local.update(Local.Sensors.TABLE_NAME, sensor_dicts)
                .where((Local.Sensors.ColName.ID,), sensor_ids)
                .execute()
            )
        if remote:
            if props is not None:
                rem_props = tuple(prop for prop in props if prop in SENSOR_FIELDS)
                rem_dicts = [
                    sensor.to_dict(rem_props, SENSOR_TO_REMOTE_MAP)
                    for sensor in module._sensors
                ]
            else:
                rem_dicts = [
                    sensor.to_dict(to_map=SENSOR_TO_REMOTE_MAP)
                    for sensor in module._sensors
                ]

            self.__remote.update_documents(
                self.PATH_SUBCOL_SENSORS[module.id],
                sensor_ids,
                rem_dicts,
            )
        return local_result

    def init_command_listener(
        self, callback: Callable[[List[Command], datetime], None]
    ):
        self.__remote.register_listener(
            self.PATH_COMMANDS,
            lambda doc_dicts, timestamp: callback(
                self.__remote.map_to_object(
                    doc_dicts,
                    Command.Props().ID,
                    lambda dic: Command.from_dict(dic, COMMAND_FROM_REMOTE_MAP),
                ),
                timestamp,
            ),
        )

    def delete_command(self, command: Command):
        self.__remote.delete_document(self.PATH_COMMANDS, command.id)

    def delete_module(self, module: EPModule):
        self.__remote.delete_document(self.PATH_MODULES, module.id)
        self._modules.pop(module.mac_id)

    def reset_databases(self):
        # TODO handle deletion of sensors prior to modules

        ret = self.__remote.delete_collection(self.PATH_MODULES)
        print("Collection {} Deleted: {}".format(Remote.Modules.COLL_NAME, ret))

        ret = self.__remote.delete_document(
            Remote.Systems.COLL_NAME, self.__get_system_id()
        )
        print("Collection {} Deleted: {}".format(Remote.Systems.COLL_NAME, ret))

        ret = self._local.delete(Local.Modules.TABLE_NAME)
        print("Table {} Deleted: {}".format(Local.Modules.TABLE_NAME, ret))

        ret = self._local.delete(Local.System.TABLE_NAME)
        print("Table {} Deleted: {}".format(Local.System.TABLE_NAME, ret))

    def disconnect(self):
        self.__remote.disconnect()


class Actions(Enum):
    REFRESH = 0
    OPEN = 1
    CLOSE = 2
    UPDATE = 3


class Command(DictParseable):
    __Attributes = namedtuple("__Attributes", "IP DURATION DESCRIPTION")
    Attrs = __Attributes("ip", "duration", "description")

    __Properties = namedtuple("__Properties", "ID ACTION TIME ATTR")
    __Props = __Properties("id", "action", "timestamp", "attributes")

    @staticmethod
    def Props() -> __Properties:
        return Command.__Props

    def __init__(
        self, id: str, time: datetime, action: Actions, **attr: Dict[str, Any]
    ) -> None:
        self._id = id
        self.timestamp = time.astimezone()
        self.action = action
        self.attributes = attr

    def __str__(self) -> str:
        to_string = "[{0}: Command]:".format(self.timestamp.strftime("%Y-%m-%d %X"))

        match self.action:
            case Actions.OPEN:
                to_string = "{0} Turn ON Valve #{1} for: {2}s".format(
                    to_string,
                    self.attributes[Command.Attrs.IP],
                    self.attributes[Command.Attrs.DURATION],
                )
            case Actions.CLOSE:
                to_string = "{0} Turn OFF Valve #{1}".format(
                    to_string, self.attributes[Command.Attrs.IP]
                )
            case Actions.UPDATE:
                to_string = "{0} Edit Valve #{1} description to: {2}".format(
                    to_string,
                    self.attributes[Command.Attrs.IP],
                    self.attributes[Command.Attrs.DESCRIPTION],
                )

        return to_string


COMMAND_FROM_REMOTE_MAP = {
    Remote.Commands.FieldName.ACTION: Command.Props().ACTION,
    Remote.Commands.FieldName.TIME: Command.Props().TIME,
    Remote.Commands.FieldName.ATTR: Command.Props().ATTR,
}
"""A Mapping of remote database 'Commands' collection fields to `repository.Command` properties (key=field_name, val=prop_name)."""
