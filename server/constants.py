""" A hierarchical view of local and remote data base's constants.
"""
from __future__ import annotations
from collections import namedtuple
from typing import NamedTuple, Tuple
from connections.sqlite import ValueType, Attributes
from extensions import get_cls_fields_values, reverse_dict

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
