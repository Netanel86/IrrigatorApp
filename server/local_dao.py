from __future__ import annotations
import logging
from typing import Any, Dict, List, Tuple
from connections.sqlite import *
from extensions import get_cls_fields_values, is_empty, reverse_dict
from infra import IDable
from model import AnalogSensor, EPModule, SensorType
from django.utils.crypto import get_random_string

# region Constants
class ColNamesConsts:
    ID = "id"
    BATCH_ID = "batch_id"


class ModuleConsts:
    class ColName:
        ID = ColNamesConsts.ID
        BATCH_ID = ColNamesConsts.BATCH_ID
        MAC_ID = "mac_id"
        DESCRIPTION = "description"
        MAX_DURATION = "max_duration"
        DURATION = "duration"
        ON_TIME = "on_time"
        PORT = "port"
        TIMEOUT = "timeout"

    TABLE_NAME: str = "module"
    """'Module' table name"""
    TYPE_MAP: Tuple[Tuple] = (
        (ColName.ID, ValueType.INT, Attributes.PRIMARY_KEY),
        (ColName.BATCH_ID, ValueType.TEXT),
        (ColName.MAC_ID, ValueType.TEXT),
        (ColName.DESCRIPTION, ValueType.TEXT),
        (ColName.MAX_DURATION, ValueType.INT),
        (ColName.DURATION, ValueType.INT),
        (ColName.ON_TIME, ValueType.TIME),
        (ColName.PORT, ValueType.INT),
        (ColName.TIMEOUT, ValueType.FLOAT),
    )
    """A map describing the column names, data types and attributes for creating the 'Module' table"""
    SELECT_COLUMNS: Tuple = tuple(
        get_cls_fields_values(ColName, exclude=[ColName.BATCH_ID])
    )
    """A set of predefined columns to SELECT a `:class:model.Module` object"""
    MAP_FROM_LOCAL = {
        ColName.ID: EPModule.Props().ID,
        ColName.MAC_ID: EPModule.Props().MAC_ID,
        ColName.DESCRIPTION: EPModule.Props().DESCRIPTION,
        ColName.MAX_DURATION: EPModule.Props().MAX_DURATION,
        ColName.DURATION: EPModule.Props().DURATION,
        ColName.ON_TIME: EPModule.Props().ON_TIME,
        ColName.PORT: EPModule.Props().PORT,
        ColName.TIMEOUT: EPModule.Props().TIMEOUT,
    }
    """A Mapping of table 'Module' columns to `model.Module` properties (key=col_name, val=prop_name)."""
    MAP_TO_LOCAL = reverse_dict(MAP_FROM_LOCAL, exclude=[EPModule.Props().ID])
    """A Mapping of `model.Module` properties to 'Module' table columns (key=prop_name, val=col_name)."""


class SensorConsts:
    class ColName:
        ID = ColNamesConsts.ID
        BATCH_ID = ColNamesConsts.BATCH_ID
        DEVICE_ID = "device_id"
        MODULE_ID = "module_id"
        TYPE = "type"
        MIN_VAL = "min_value"
        MAX_VAL = "max_value"
        CURR_VAL = "current_value"

    TABLE_NAME: str = "sensor"
    SELECT_COLUMNS: Tuple = tuple(
        get_cls_fields_values(
            ColName,
            exclude=[ColName.BATCH_ID, ColName.MODULE_ID],
        )
    )
    """A set of predefined columns to SELECT a `:class:model.Sensor` object"""
    TYPE_MAP = (
        (ColName.ID, ValueType.INT, Attributes.PRIMARY_KEY),
        (ColName.BATCH_ID, ValueType.TEXT),
        (ColName.DEVICE_ID, ValueType.TEXT),
        (
            ColName.MODULE_ID,
            ValueType.INT,
            Attributes.ForeignKey(
                ModuleConsts.TABLE_NAME,
                ModuleConsts.ColName.ID,
                (
                    (
                        Attributes.ForeignKey.Action.ON.DELETE,
                        Attributes.ForeignKey.Action.CASCADE,
                    ),
                ),
            ),
        ),
        (ColName.TYPE, ValueType.TEXT),
        (ColName.MIN_VAL, ValueType.FLOAT),
        (ColName.MAX_VAL, ValueType.FLOAT),
        (ColName.CURR_VAL, ValueType.FLOAT),
    )
    """A map describing the column names, data types and attributes for creating the 'Sensor' table"""
    MAP_FROM_LOCAL = {
        ColName.ID: AnalogSensor.Props().ID,
        ColName.DEVICE_ID: AnalogSensor.Props().DEVICE_ID,
        ColName.TYPE: AnalogSensor.Props().TYPE,
        ColName.MIN_VAL: AnalogSensor.Props().MIN_VALUE,
        ColName.MAX_VAL: AnalogSensor.Props().MAX_VALUE,
        ColName.CURR_VAL: AnalogSensor.Props().CURRENT_VAL,
    }
    """A Mapping of table 'Sensor' columns to `model.Sensor` properties (key=col_name, val=prop_name)."""
    MAP_TO_LOCAL = reverse_dict(MAP_FROM_LOCAL, exclude=[AnalogSensor.Props().ID])
    """A Mapping of `model.Sensor` properties to 'Sensor' table columns (key=prop_name, val=col_name)."""


class XRefConsts:
    class ColName:
        TYPE = "type"
        LOCAL_ID = "local_id"
        REMOTE_ID = "remote_id"

    TABLE_NAME: str = "xref"
    COLUMNS: Tuple = tuple(get_cls_fields_values(ColName))
    TYPE_MAP: Tuple[Tuple] = (
        (ColName.TYPE, ValueType.TEXT),
        (ColName.LOCAL_ID, ValueType.INT),
        (ColName.REMOTE_ID, ValueType.TEXT),
    )


class Constants:
    ColNames = ColNamesConsts
    Module = ModuleConsts
    Sensor = SensorConsts
    XRef = XRefConsts


# endregion


class LocalDAO(object):
    DB_PATH = os.path.join(
        os.path.dirname(os.path.abspath(__file__).split("connections")[0]),
        "sqlite\db\pysqlite.db",
    )

    def __init__(self) -> None:
        self._local = SQLiteConnection(LocalDAO.DB_PATH)
        self._logger = logging.getLogger(self.__class__.__name__)
        self._build_tables()

    def _build_tables(self):
        self._local.create(Constants.Module.TABLE_NAME, Constants.Module.TYPE_MAP)
        self._local.create(Constants.Sensor.TABLE_NAME, Constants.Sensor.TYPE_MAP)
        self._local.create(Constants.XRef.TABLE_NAME, Constants.XRef.TYPE_MAP)

    def _parse_modules(self, col_data: Tuple[Tuple], values: List[Tuple]):
        mod_col_count = len(Constants.Module.SELECT_COLUMNS)
        sen_col_last_idx = mod_col_count + len(Constants.Sensor.SELECT_COLUMNS)

        modules: Dict[str, EPModule] = {}
        module_cols = col_data[:mod_col_count]
        sensor_cols = col_data[mod_col_count:sen_col_last_idx]

        for tup in values:
            module_dict = self._local.merge_to_dict(module_cols, tup[:mod_col_count])
            sensor_dict = self._local.merge_to_dict(
                sensor_cols, tup[mod_col_count:sen_col_last_idx]
            )

            module_mac = module_dict[Constants.Module.ColName.MAC_ID]
            if module_mac not in modules.keys():
                modules[module_mac] = EPModule.from_dict(
                    module_dict, Constants.Module.MAP_FROM_LOCAL
                )

            if sensor_dict[Constants.Sensor.ColName.ID] is not None:
                sensor = AnalogSensor.from_dict(
                    sensor_dict, Constants.Sensor.MAP_FROM_LOCAL
                )
                modules[module_mac].add_sensors(sensor)

        return modules

    def get_modules(self) -> Dict[str, EPModule]:
        return (
            self._local.select(
                Constants.Module.TABLE_NAME, Constants.Module.SELECT_COLUMNS
            )
            .join(
                Constants.Sensor.TABLE_NAME,
                Constants.Sensor.SELECT_COLUMNS,
                QueryBuilder.Join.LEFT,
                src_col=Constants.Module.ColName.ID,
                target_col=Constants.Sensor.ColName.MODULE_ID,
            )
            .orderby((Constants.Module.ColName.MAC_ID,))
            .execute(self._parse_modules)
        )

    def add_module(self, module: EPModule):
        module.id = self._local.insert(
            Constants.Module.TABLE_NAME,
            module.to_dict(to_map=Constants.Module.MAP_TO_LOCAL),
            Constants.Module.ColName.ID,
        )

        self.add_sensors(module.id, module.sensors)
        return module.id

    def add_modules(self, modules: List[EPModule]) -> bool:
        module_dicts: List[Dict] = []
        batch_id = get_random_string(8)
        for module in modules:
            module_dict = module.to_dict(to_map=Constants.Module.MAP_TO_LOCAL)
            module_dict[Constants.Module.ColName.BATCH_ID] = batch_id
            module_dicts.append(module_dict)

        inserted_count = self._local.insert(Constants.Module.TABLE_NAME, module_dicts)
        if inserted_count == len(module_dicts):
            result_modules = self._assign_ids(
                modules, batch_id, Constants.Module.TABLE_NAME
            )
        else:
            raise InsertError(
                self,
                self.add_modules.__name__,
                inserted=inserted_count,
                target=len(module_dicts),
            )

        all_sensors: List[List[SensorType]] = []
        module_ids = []
        for module in modules:
            module_ids.append(module.id)
            all_sensors.append(module.sensors if not is_empty(module.sensors) else None)

        result_sensors = self.add_many_sensors(module_ids, all_sensors)

        return result_modules and result_sensors

    def add_sensors(self, module_id: int, sensors: List[AnalogSensor]) -> bool:
        if not is_empty(sensors):
            new_sensors = [sensor for sensor in sensors if sensor.id is None]

            sens_dicts = []
            batch_id = get_random_string(8)
            for sensor in new_sensors:
                sens_dict = sensor.to_dict(to_map=Constants.Sensor.MAP_TO_LOCAL)
                sens_dict[Constants.Sensor.ColName.MODULE_ID] = module_id
                sens_dict[Constants.Sensor.ColName.BATCH_ID] = batch_id
                sens_dicts.append(sens_dict)

            inserted_count = self._local.insert(
                Constants.Sensor.TABLE_NAME,
                sens_dicts,
            )

            if inserted_count == len(sens_dicts):
                result = self._assign_ids(
                    new_sensors, batch_id, Constants.Sensor.TABLE_NAME
                )
            else:
                raise InsertError(
                    self,
                    self.add_sensors.__name__,
                    inserted=inserted_count,
                    target=len(sens_dicts),
                )

            return result

    def add_many_sensors(
        self,
        module_ids: List[str],
        sensor_lists: List[List[AnalogSensor]],
    ):
        new_sensor_lists: List[List[AnalogSensor]] = []

        for sensors in sensor_lists:
            new_sensor_lists.append(
                [sensor for sensor in sensors if sensor.id is None] if sensors else None
            )

        sens_dicts = []
        assign_list: List[AnalogSensor] = []
        batch_id = get_random_string(8)
        for idx, sensors in enumerate(new_sensor_lists):
            if sensors:
                for sensor in sensors:
                    sens_dict = sensor.to_dict(to_map=Constants.Sensor.MAP_TO_LOCAL)
                    sens_dict[Constants.Sensor.ColName.MODULE_ID] = module_ids[idx]
                    sens_dict[Constants.Sensor.ColName.BATCH_ID] = batch_id
                    sens_dicts.append(sens_dict)
            assign_list.extend(sensors)

        inserted_count = self._local.insert(Constants.Sensor.TABLE_NAME, sens_dicts)
        if inserted_count == len(sens_dicts):
            result = self._assign_ids(
                assign_list, batch_id, Constants.Sensor.TABLE_NAME
            )
        else:
            raise InsertError(
                self,
                self.add_many_sensors.__name__,
                inserted=inserted_count,
                target=len(sens_dicts),
            )
        return result

    def update_module(self, module: EPModule, props: List[str] = None):
        module_dict = (
            module.to_dict(props, Constants.Module.MAP_TO_LOCAL)
            if props
            else module.to_dict(to_map=Constants.Module.MAP_TO_LOCAL)
        )

        local_done = (
            self._local.update(Constants.Module.TABLE_NAME, module_dict)
            .where({Constants.Module.ColName.ID: module.id})
            .execute()
        )

        return local_done

    def update_sensor(self, sensor: AnalogSensor, props: List[str] = None):
        sensor_dict = (
            sensor.to_dict(props, Constants.Sensor.MAP_TO_LOCAL)
            if props
            else sensor.to_dict(to_map=Constants.Sensor.MAP_TO_LOCAL)
        )

        result = (
            self._local.update(Constants.Sensor.TABLE_NAME, sensor_dict)
            .where({Constants.Sensor.ColName.ID: sensor.id})
            .execute()
        )

        return result

    def update_sensors(self, sensors: List[AnalogSensor], props: List[str] = None):
        sensor_ids = [sensor.id for sensor in sensors]
        sensor_dicts: List[Dict[str, Any]] = None

        sensor_dicts = [
            sensor.to_dict(props, Constants.Sensor.MAP_TO_LOCAL)
            if props
            else sensor.to_dict(to_map=Constants.Sensor.MAP_TO_LOCAL)
            for sensor in sensors
        ]

        local_result = (
            self._local.update(Constants.Sensor.TABLE_NAME, sensor_dicts)
            .where((Constants.Sensor.ColName.ID,), sensor_ids)
            .execute()
        )

        return local_result

    def _assign_ids(self, objects: List[IDable], batch_id: str, table: str, **names):
        colname_id = names.get("id", Constants.ColNames.ID)
        colname_batch = names.get("batch", Constants.ColNames.BATCH_ID)

        result_dicts: List[Dict[str, Any]] = (
            self._local.select(table, (colname_id,))
            .where((colname_batch,), (batch_id,))
            .execute(PARSER.DICT)
        )

        for obj, object_dict in zip(objects, result_dicts):
            obj.id = object_dict[colname_id]

        return not is_empty(result_dicts)

    def delete_module(self, module_id: int) -> bool:
        return self._local.delete(Constants.Module.TABLE_NAME, module_id)

    def purge(self):
        def log_delete_msg(name, result):
            self._logger.info(
                f"{self.purge.__name__}> Delete table {name} returned result: '{result}'"
            )

        ret = self._local.delete(Constants.Sensor.TABLE_NAME)
        log_delete_msg(Constants.Sensor.TABLE_NAME, ret)

        ret = self._local.delete(Constants.Module.TABLE_NAME)
        log_delete_msg(Constants.Module.TABLE_NAME, ret)

        ret = self._local.delete(Constants.XRef.TABLE_NAME)
        log_delete_msg(Constants.XRef.TABLE_NAME, ret)


class InsertError(Exception):
    def __init__(self, sender: object, method: str, **args) -> None:
        self.inserted_count = args.get("inserted", None)
        self.target_count = args.get("target", None)
        self.message = f"{sender.__class__.__name__}.{method}> Failed to insert rows"

        if self.inserted_count and self.target_count:
            self.message += (
                f", inserted '{self.inserted_count}' out of '{self.target_count}' rows."
            )
        super().__init__(self.message)
