""" A hierarchical view of local and remote data base's constants.
"""
from __future__ import annotations
from collections import namedtuple
from typing import NamedTuple, Tuple
from data.sqlite import TYPE, ATTR

# region Remote constants
__RemCollections = namedtuple("__RemColls", "SYSTEMS MODULES COMMANDS SENSORS")
__REM_COLLS = __RemCollections("systems", "modules", "commands", "sensors")

__ModuleRemFields = namedtuple(
    "__ModuleRemFields", "MASTER_ID IP DESCRIPTION MAX_DURATION DURATION ON_TIME"
)
__REM_MOD_FIELDS = __ModuleRemFields(
    "masterId", "ip", "description", "maxDuration", "duration", "onTime"
)

__SensorRemFields = namedtuple("__SensorRemFields", "TYPE MIN_VAL MAX_VAL CURR_VAL")
__REM_SENS_FIELDS = __SensorRemFields("type", "minVal", "maxVal", "currVal")

__SystemRemFields = namedtuple("__SystemRemFields", "ID")
__REM_SYS_FIELDS = __SystemRemFields("id")

__CommRemFields = namedtuple("__CommRemFields", "ACTION TIME ATTR")
__REM_COMM_FIELDS = __CommRemFields("action", "timestamp", "attributes")


class __ModuleRem(NamedTuple):
    FieldName: __ModuleRemFields
    FIELDS: Tuple[str]
    COLL_NAME: str


__REM_MODULE = __ModuleRem(
    __REM_MOD_FIELDS, tuple(__REM_MOD_FIELDS), __REM_COLLS.MODULES
)


class __SensorRem(NamedTuple):
    FieldName: __SensorRemFields
    FIELDS: Tuple[str]
    COLL_NAME: str


__REM_SENS = __SensorRem(
    __REM_SENS_FIELDS, tuple(__REM_SENS_FIELDS), __REM_COLLS.SENSORS
)


class __CommandRem(NamedTuple):
    FieldName: __CommRemFields
    COLL_NAME: str


__REM_COMM = __CommandRem(__REM_COMM_FIELDS, __REM_COLLS.COMMANDS)


class __SystemRem(NamedTuple):
    FieldName: __SystemRemFields
    COLL_NAME: str


__REM_SYS = __SystemRem(__REM_SYS_FIELDS, __REM_COLLS.SYSTEMS)


class __Remote(NamedTuple):
    Commands: __CommandRem
    Modules: __ModuleRem
    Sensors: __SensorRem
    Systems: __SystemRem


Remote = __Remote(__REM_COMM, __REM_MODULE, __REM_SENS, __REM_SYS)
""" A hierarchical view on remote data base's collections and field names.
"""
# endregion

# region Local constants
__LocalTableNames = namedtuple("__LocalTables", "system module sensor xref")
__local_table_names = __LocalTableNames("system", "module", "sensor", "xref")

__LocalModuleColumns = namedtuple(
    "__LocalModuleColumns",
    "id mac_id description max_duration duration on_time port timeout",
)
__local_module_columns = __LocalModuleColumns(
    "module_id",
    "mac_id",
    "description",
    "max_duration",
    "duration",
    "on_time",
    "port",
    "timeout",
)
__local_module_types = (
    (__local_module_columns.id, TYPE.INT, ATTR.PRIMARY_KEY),
    (__local_module_columns.mac_id, TYPE.TEXT),
    (__local_module_columns.description, TYPE.TEXT),
    (__local_module_columns.max_duration, TYPE.INT),
    (__local_module_columns.duration, TYPE.INT),
    (__local_module_columns.on_time, TYPE.TIME),
    (__local_module_columns.port, TYPE.INT),
    (__local_module_columns.timeout, TYPE.FLOAT),
)

__LocalSensorColumns = namedtuple(
    "__LocalSensorColumns", "id module_id type min_val max_val curr_val"
)
__local_sensor_columns = __LocalSensorColumns(
    "id", "module_id", "type", "min_value", "max_value", "current_value"
)
__local_sensor_types = (
    (__local_sensor_columns.id, TYPE.TEXT),
    (__local_sensor_columns.module_id, TYPE.TEXT),
    (__local_sensor_columns.type, TYPE.TEXT),
    (__local_sensor_columns.min_val, TYPE.FLOAT),
    (__local_sensor_columns.max_val, TYPE.FLOAT),
    (__local_sensor_columns.curr_val, TYPE.FLOAT),
)

__LocalXrefColumns = namedtuple("__LocalXrefColumns", "type local_id remote_id")
__local_xref_columns = __LocalXrefColumns("type", "local_id", "remote_id")
__local_xref_types = (
    (__local_xref_columns.type, TYPE.TEXT),
    (__local_xref_columns.local_id, TYPE.INT),
    (__local_xref_columns.remote_id, TYPE.TEXT),
)

__LocalSystemColumns = namedtuple("__SystemLocColumns", "id")
__local_system_columns = __LocalSystemColumns("id")
__local_system_types = ((__local_system_columns.id, TYPE.TEXT),)


class __LocalTableSensor(NamedTuple):
    column_name: __LocalSensorColumns
    columns: Tuple
    table_name: str
    type_map: Tuple[Tuple]


__local_sensor_table = __LocalTableSensor(
    __local_sensor_columns,
    tuple(__local_sensor_columns),
    __local_table_names.sensor,
    __local_sensor_types,
)


class __LocalTableModule(NamedTuple):
    column_name: __LocalModuleColumns
    columns: Tuple
    table_name: str
    type_map: Tuple[Tuple]


__local_module_table = __LocalTableModule(
    __local_module_columns,
    tuple(__local_module_columns),
    __local_table_names.module,
    __local_module_types,
)


class __LocalTableXref(NamedTuple):
    column_name: __LocalXrefColumns
    columns: Tuple
    table_name: str
    type_map: Tuple[Tuple]


__local_xref_table = __LocalTableXref(
    __local_xref_columns,
    tuple(__local_xref_columns),
    __local_table_names.xref,
    __local_xref_types,
)


class __LocalTableSystem(NamedTuple):
    column_name: __LocalSystemColumns
    columns: Tuple
    table_name: str
    type_map: Tuple[Tuple]


__local_system_table = __LocalTableSystem(
    __local_system_columns,
    tuple(__local_system_columns),
    __local_table_names.system,
    __local_system_types,
)


class __LocalTables(NamedTuple):
    module: __LocalTableModule
    sensor: __LocalTableSensor
    system: __LocalTableSystem
    xref: __LocalTableXref


local_tables = __LocalTables(
    __local_module_table, __local_sensor_table, __local_system_table, __local_xref_table
)
# endregion
