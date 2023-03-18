from typing import Tuple
from connections.firestore import FirestoreConnection, OrderBy
from extensions import get_cls_fields_values, reverse_dict
from model import *

# region Constants
class ModulesConsts:
    class FieldName:
        IP = "ip"
        DESCRIPTION = "description"
        MAX_DURATION = "maxDuration"
        DURATION = "duration"
        ON_TIME = "onTime"

    COLLECTION_NAME: str = "Modules"
    """'Modules' collection name"""

    FIELDS: Tuple[str, ...] = get_cls_fields_values(FieldName)
    """A tuple of 'Modules' collection fields"""

    MAP_FROM_REMOTE: Dict[str, str] = {
        FieldName.IP: EPModule.Props().MAC_ID,
        FieldName.DESCRIPTION: EPModule.Props().DESCRIPTION,
        FieldName.MAX_DURATION: EPModule.Props().MAX_DURATION,
        FieldName.DURATION: EPModule.Props().DURATION,
        FieldName.ON_TIME: EPModule.Props().ON_TIME,
    }
    """A Mapping of remote database 'Modules' collection fields to `ModelLib.EPModule` properties (key=field_name, val=prop_name)."""

    MAP_TO_REMOTE: Dict[str, str] = reverse_dict(MAP_FROM_REMOTE)
    """A Mapping of `ModelLib.EPModule` properties to remote database 'Modules' collection fields (key=prop_name, val=field_name)."""


class SensorsConsts:
    class FieldName:
        TYPE = "type"
        MIN_VAL = "minVal"
        MAX_VAL = "maxVal"
        CURR_VAL = "currVal"

    COLLECTION_NAME: str = "Sensors"

    FIELDS: Tuple[str, ...] = get_cls_fields_values(FieldName)
    """A tuple of 'Sensors' collection fields"""

    MAP_FROM_REMOTE: Dict[str, str] = {
        FieldName.TYPE: AnalogSensor.Props().TYPE,
        FieldName.MIN_VAL: AnalogSensor.Props().MIN_VALUE,
        FieldName.MAX_VAL: AnalogSensor.Props().MAX_VALUE,
        FieldName.CURR_VAL: AnalogSensor.Props().CURRENT_VAL,
    }
    """A Mapping of remote database 'Sensors' collection fields to `model.Sensor` properties (key=field_name, val=prop_name)."""

    MAP_TO_REMOTE: Dict[str, str] = reverse_dict(MAP_FROM_REMOTE)
    """A Mapping of `model.Sensor` properties to remote database 'Sensors' collection fields (key=prop_name, val=field_name)."""


class CommandsConsts:
    class FieldName:
        ACTION = "action"
        TIME = "timestamp"
        ATTR = "attributes"

    COLLECTION_NAME: str = "Commands"

    MAP_FROM_REMOTE = {
        FieldName.ACTION: Command.Props().ACTION,
        FieldName.TIME: Command.Props().TIME,
        FieldName.ATTR: Command.Props().ATTR,
    }
    """A Mapping of remote database 'Commands' collection fields to `model.Command` properties (key=field_name, val=prop_name)."""


class SystemsConsts:
    COLLECTION_NAME: str = "Systems"


class Constants:
    Modules = ModulesConsts
    Sensors = SensorsConsts
    Commands = CommandsConsts
    Systems = SystemsConsts


# endregion


class RemoteDAO:
    def __init__(self, system_id: str = None) -> None:
        self.logger = logging.getLogger(self.__class__.__name__)
        self.__db = FirestoreConnection()
        self.SYSTEM_ID = self.generate_system_id() if system_id is None else system_id
        self.__init_paths()

    def __init_paths(self):
        if self.SYSTEM_ID is None:
            raise ValueError(
                "system_id: value can't be None, call get_system_id() before initializing paths"
            )

        self.PATH_SYSTEM = f"{Constants.Systems.COLLECTION_NAME}/{self.SYSTEM_ID}"
        self.PATH_COMMANDS = f"{self.PATH_SYSTEM}/{Constants.Commands.COLLECTION_NAME}"
        self.PATH_MODULES = f"{self.PATH_SYSTEM}/{Constants.Modules.COLLECTION_NAME}"

        self.PATH_SUBCOL_SENSORS = {}

    def generate_system_id(self) -> str:
        """Generates a new system id.

        Returns:
            (`str`): A new system id.
        """
        return self.__db.document_ref(Constants.Systems.COLLECTION_NAME)

    def add_module(self, module: EPModule):
        module_rem_id = self.__db.add_document(
            self.PATH_MODULES, module.to_dict(to_map=Constants.Modules.MAP_TO_REMOTE)
        )

        sens_rem_ids = (
            self.add_sensors(module_rem_id, module.sensors)
            if not is_empty(module.sensors)
            else None
        )

        return (module_rem_id, sens_rem_ids)

    def __associate_ids(self, object_list: List[IDable], remote_ids: List[str]):
        """Creates a dictionary from two lists where each object id is associated to a remote id"""
        return {
            _object.id: remote_id for _object, remote_id in zip(object_list, remote_ids)
        }

    def add_sensors(self, module_id: str, sensors: List[AnalogSensor]):
        sensors_dicts = [
            sensor.to_dict(to_map=Constants.Sensors.MAP_TO_REMOTE) for sensor in sensors
        ]

        if self.PATH_SUBCOL_SENSORS.get(module_id, None) == None:
            self.PATH_SUBCOL_SENSORS[
                module_id
            ] = f"{self.PATH_MODULES}/{module_id}/{Constants.Sensors.COLLECTION_NAME}"

        sens_ids = self.__db.add_documents(
            self.PATH_SUBCOL_SENSORS[module_id], sensors_dicts
        )

        return (module_id, sens_ids)

    def add_many_sensors(
        self,
        module_ids: List[str],
        sensor_lists: List[List[AnalogSensor]],
    ) -> List[Tuple[str, List[str]]]:
        result_ids: List[Tuple[str, List[str]]] = []
        for idx, sensors in enumerate(sensor_lists):
            sens_rem_dicts = []
            if sensors:
                sens_rem_dicts = [
                    sensor.to_dict(to_map=Constants.Sensors.MAP_TO_REMOTE)
                    for sensor in sensors
                ]
                sens_rem_ids = self.add_sensors(module_ids[idx], sens_rem_dicts)
                result_ids.append((module_ids[idx], sens_rem_ids))

        return result_ids

    def add_modules(self, modules: List[EPModule]) -> Dict[str, List[str]]:
        """
        Returns:
            (`Dict[str, List[str]]`): Dictionary with module remote id and a list of sensor id's as key: value pairs,
            example: (module_rem_id: sensor_ids)
        """
        module_dicts = []
        for module in modules:
            module_dicts.append(module.to_dict(to_map=Constants.Modules.MAP_TO_REMOTE))

        module_rem_ids = self.__db.add_documents(self.PATH_MODULES, module_dicts)
        result = {mod_rem_id: None for mod_rem_id in module_rem_ids}

        all_sensors: List[List[AnalogSensor]] = []
        module_ids_wSensor = []
        for idx, module in enumerate(modules):
            if not is_empty(module.sensors):
                module_ids_wSensor.append(module_rem_ids[idx])
                all_sensors.append(module.sensors)

        remote_ids = self.add_many_sensors(module_ids_wSensor, all_sensors)
        MODULE_REM_ID = 0
        SENSOR_REM_IDS = 1
        for remote_id_tuple in remote_ids:
            result[remote_id_tuple[MODULE_REM_ID]] = remote_id_tuple[SENSOR_REM_IDS]

        return result

    def get_commands(self) -> List[Command]:
        cmnd_dicts = self.__db.get_collection(
            self.PATH_COMMANDS,
            OrderBy(Constants.Commands.FieldName.TIME, OrderBy.DESCENDING),
        )
        return self.__db.map_to_object(
            cmnd_dicts,
            Command.Props().ID,
            lambda dic: Command.from_dict(dic, Constants.Commands.MAP_FROM_REMOTE),
        )

    def update_module(self, module_id: str, module: EPModule, props: List[str] = None):
        if props is not None:
            rem_props = tuple(
                prop for prop in props if prop in Constants.Modules.FIELDS
            )
            module_dict = module.to_dict(rem_props, Constants.Modules.MAP_TO_REMOTE)
        else:
            module_dict = module.to_dict(to_map=Constants.Modules.MAP_TO_REMOTE)

        self.__db.update_document(self.PATH_MODULES, module_id, module_dict)

    def update_sensor(
        self,
        module_id: str,
        sensor_id: str,
        sensor: AnalogSensor,
        props: List[str] = None,
        **kwargs,
    ):
        if props is not None:
            rem_props = tuple(
                prop for prop in props if prop in Constants.Sensors.FIELDS
            )
            rem_dict = sensor.to_dict(rem_props, Constants.Sensors.MAP_TO_REMOTE)
        else:
            rem_dict = sensor.to_dict(to_map=Constants.Sensors.MAP_TO_REMOTE)

        self.__db.update_document(
            self.PATH_SUBCOL_SENSORS[module_id],
            sensor_id,
            rem_dict,
        )

    def update_sensors(
        self,
        module_id: str,
        sensor_ids: List[str],
        module: EPModule,
        props: List[str] = None,
    ):
        if props is not None:
            rem_props = tuple(
                prop for prop in props if prop in Constants.Sensors.FIELDS
            )
            sensor_dicts = [
                sensor.to_dict(rem_props, Constants.Sensors.MAP_TO_REMOTE)
                for sensor in module.sensors
            ]
        else:
            sensor_dicts = [
                sensor.to_dict(to_map=Constants.Sensors.MAP_TO_REMOTE)
                for sensor in module.sensors
            ]

        self.__db.update_documents(
            self.PATH_SUBCOL_SENSORS[module_id],
            sensor_ids,
            sensor_dicts,
        )

    def init_command_listener(
        self, callback: Callable[[List[Command], datetime], None]
    ):
        self.__db.register_listener(
            self.PATH_COMMANDS,
            lambda doc_dicts, timestamp: callback(
                self.__db.map_to_object(
                    doc_dicts,
                    Command.Props().ID,
                    lambda dic: Command.from_dict(
                        dic, Constants.Commands.MAP_FROM_REMOTE
                    ),
                ),
                timestamp,
            ),
        )

    def delete_command(self, command: Command):
        self.__db.delete_document(self.PATH_COMMANDS, command.id)

    def delete_module(self, module_id: str):
        self.__db.delete_collection(self.PATH_SUBCOL_SENSORS[module_id])
        self.__db.delete_document(self.PATH_MODULES, module_id)

    def delete_sensor(self, module_id:str, sensor_id: str):
        self.__db.delete_document(self.PATH_SUBCOL_SENSORS[module_id],sensor_id)

    def purge(self):
        def log_delete_msg(name, result):
            self.logger.info(
                f"{self.purge.__name__}> Delete collection {name} returned result: '{result}'"
            )
        for module_id, path in self.PATH_SUBCOL_SENSORS.items():
            result = self.__db.delete_collection(path)
            log_delete_msg(f"{Constants.Sensors.COLLECTION_NAME} under module: {module_id},", result)

        result = self.__db.delete_collection(self.PATH_MODULES)
        log_delete_msg(Constants.Modules.COLLECTION_NAME, result)

        result = self.__db.delete_collection(self.PATH_COMMANDS)
        log_delete_msg(Constants.Commands.COLLECTION_NAME, result)

        result = self.__db.delete_document(Constants.Systems.COLLECTION_NAME, self.SYSTEM_ID)
        log_delete_msg(Constants.Systems.COLLECTION_NAME, result)

        self.PATH_SUBCOL_SENSORS.clear()
        self.PATH_MODULES = None
        self.PATH_COMMANDS = None
        self.PATH_SYSTEM = None
