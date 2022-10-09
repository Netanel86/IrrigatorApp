""" A hierarchical view of local and remote data base's constants.
"""
from __future__ import annotations
from collections import namedtuple
from typing import NamedTuple, Tuple
from sqlite import TYPES

# region Remote constants
__RemCollections = namedtuple("__RemColls", "SYSTEMS MODULES COMMANDS SENSORS")
__REM_COLLS = __RemCollections("systems", "modules", "commands", "sensors")

__ModuleRemFields = namedtuple(
    "__ModuleRemFields", "ID IP DESCRIPTION MAX_DURATION DURATION ON_TIME"
)
__REM_MOD_FIELDS = __ModuleRemFields(
    "id", "ip", "description", "maxDuration", "duration", "onTime"
)

__SensorRemFields = namedtuple(
    "__SensorRemFields", "ID TYPE MIN_VAL MAX_VAL CURR_VAL"
)
__REM_SENS_FIELDS = __SensorRemFields("id", "type", "minVal", "maxVal", "currVal")

__SystemRemFields = namedtuple("__SystemRemFields", "ID")
__REM_SYS_FIELDS = __SystemRemFields("id")

__CommRemFields = namedtuple("__CommRemFields", "ID ACTION TIME ATTR")
__REM_COMM_FIELDS = __CommRemFields("id", "action", "timestamp", "attributes")


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
__LocTables = namedtuple("__LocTableNames", "SYSTEM MODULES SENSORS")
__LOC_TABLES = __LocTables("system", "modules", "sensors")

__ModuleLocColumns = namedtuple(
    "__ModuleLocColumns", "ID IP DESCRIPTION MAX_DURATION DURATION ON_TIME PORT TIMEOUT"
)
__LOC_MOD_COLUMNS = __ModuleLocColumns(
    "id", "ip", "description", "max_duration", "duration", "on_time", "port", "timeout"
)
__LOC_MOD_TYPES = (
    (__LOC_MOD_COLUMNS.ID, TYPES.TEXT),
    (__LOC_MOD_COLUMNS.IP, TYPES.TEXT),
    (__LOC_MOD_COLUMNS.DESCRIPTION, TYPES.TEXT),
    (__LOC_MOD_COLUMNS.MAX_DURATION, TYPES.INT),
    (__LOC_MOD_COLUMNS.DURATION, TYPES.INT),
    (__LOC_MOD_COLUMNS.ON_TIME, TYPES.TIME),
    (__LOC_MOD_COLUMNS.PORT, TYPES.INT),
    (__LOC_MOD_COLUMNS.TIMEOUT, TYPES.FLOAT),
)

__SensorLocColumns = namedtuple(
    "__SensorLocColumns", "ID MODULE_ID TYPE MIN_VAL MAX_VAL CURR_VAL"
)
__LOC_SENS_COLUMNS = __SensorLocColumns(
    "id", "module_id", "type", "min_val", "max_val", "curr_val"
)
__LOC_SENS_TYPES = (
    (__LOC_SENS_COLUMNS.ID, TYPES.TEXT),
    (__LOC_SENS_COLUMNS.MODULE_ID, TYPES.TEXT),
    (__LOC_SENS_COLUMNS.TYPE, TYPES.TEXT),
    (__LOC_SENS_COLUMNS.MIN_VAL, TYPES.FLOAT),
    (__LOC_SENS_COLUMNS.MAX_VAL, TYPES.FLOAT),
    (__LOC_SENS_COLUMNS.CURR_VAL, TYPES.FLOAT),
)

__SystemLocColumns = namedtuple("__SystemLocColumns", "ID")
__LOC_SYS_COLUMNS = __SystemLocColumns("id")
__LOC_SYS_TYPES = ((__LOC_SYS_COLUMNS.ID, TYPES.TEXT),)


class __SensorLoc(NamedTuple):
    ColName: __SensorLocColumns
    COLUMNS: Tuple
    TABLE_NAME: str
    TYPE_MAP: Tuple[Tuple]


__LOC_SENS = __SensorLoc(
    __LOC_SENS_COLUMNS,
    tuple(__LOC_SENS_COLUMNS),
    __LOC_TABLES.SENSORS,
    __LOC_SENS_TYPES,
)


class __ModuleLoc(NamedTuple):
    ColName: __ModuleLocColumns
    COLUMNS: Tuple
    TABLE_NAME: str
    TYPE_MAP: Tuple[Tuple]


__LOC_MOD = __ModuleLoc(
    __LOC_MOD_COLUMNS,
    tuple(__LOC_MOD_COLUMNS),
    __LOC_TABLES.MODULES,
    __LOC_MOD_TYPES,
)


class __SystemLoc(NamedTuple):
    ColName: __SystemLocColumns
    COLUMNS: Tuple
    TABLE_NAME: str
    TYPE_MAP: Tuple[Tuple]


__LOC_SYS = __SystemLoc(
    __LOC_SYS_COLUMNS, tuple(__LOC_SYS_COLUMNS), __LOC_TABLES.SYSTEM, __LOC_SYS_TYPES
)


class __Local(NamedTuple):
    Modules: __ModuleLoc
    Sensors: __SensorLoc
    System: __SystemLoc


Local = __Local(__LOC_MOD, __LOC_SENS, __LOC_SYS)
# endregion
