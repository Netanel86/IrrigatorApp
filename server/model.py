from __future__ import annotations
from abc import ABC
from collections import namedtuple
from enum import Enum
import logging
from datetime import datetime
from typing import Any, Dict, List, NamedTuple, Tuple, Callable


class Observable(object):
    def __init__(self) -> None:
        self.__callbacks: List[Callable[[Observable, str, Any, Any], None]] = None

    def register_callback(self, callback: Callable[[Observable, str, Any, Any], None]):
        if self.__callbacks is None:
            self.__callbacks = []
        self.__callbacks.append(callback)

    def unregister_callback(
        self, callback: Callable[[Observable, str, Any, Any], None]
    ):
        self.__callbacks.remove(callback)
        if len(self.__callbacks) == 0:
            self.__callbacks.clear()
            self.__callbacks = None

    def _notify_change(self, property: str, old_value, new_value):
        if old_value != new_value:
            if self.__callbacks is not None:
                for callback in self.__callbacks:
                    callback(self, property, old_value, new_value)


class DictParseable(ABC):
    @classmethod
    def Props(cls) -> NamedTuple:
        """An abstract representation of class property names.

        To be implemented and initialized with a list of derived class property names.
        """
        raise NotImplementedError(
            f"property getter not implemented in class {cls.__class__.__name__}.Props()"
        )

    @classmethod
    def from_dict(
        cls, source: Dict[str, Any], from_map: Dict[str, str] = None
    ) -> DictParseable:
        """Parses and creates a new object from a dictionary.

        Args:
            source: a dictionary with object's properties (name: value) pairs, with to initiate the new object.
                possible values: included in `DictParseable.Props()`.

            from_map(optional): a dictionary mapping the source properties to this class properties,
                (src_prop_x: object_prop_x) pairs. use when the source and object propery names are not identical.
                default: None.

        Returns:
            `DictParseable`: the child object derived from `DictParseable`, initialized with dictionary data.

        Raises:
            `AttributeError`: if object does not contain a property name.
        """
        module = cls()
        module.update_dict(source, from_map)
        return module

    @classmethod
    def _raiseAttributeError(cls, method_name, prop_name):
        raise AttributeError(
            f"'{cls.__name__}.{method_name}()': has no attribute: '{prop_name}'"
        )

    @property
    def id(self) -> str:
        raise NotImplementedError(
            f"'id': property getter not implemented in class '{self.__class__.__name__}'"
        )

    @id.setter
    def id(self, value: str):
        raise NotImplementedError(
            f"'id': property setter not implemented in class '{self.__class__.__name__}'"
        )

    def to_dict(
        self, props: Tuple[str] = None, to_map: Dict[str, str] = None
    ) -> Dict[str, Any]:
        """Parses the 'DictParseable' object to a dictionary.

        Args:
            * `props`(optional) -- a list of properties names to parse, if set only the specified properties would be parsed,
                * possible values: included in `DictParseable.Props()`, default: `None`.
            * `from_map`(optional) -- a dictionary mapping this class properties to a set of custom properties,
                (cls_prop_x: trg_prop_x) pairs. use when custom property names are needed.
                default: None.

        Returns:
            A dictionary with the object's properties, (name: value) pairs

        Raises:
            `AttributeError`: if object does not contain one of the property names in `props` or `from_map`.
        """
        prop_dict = {}
        is_map = to_map is not None
        is_props = props is not None

        collection = props if is_props else to_map.keys() if is_map else None

        if collection is not None:
            for prop in collection:
                if not hasattr(self, prop):
                    DictParseable._raiseAttributeError(getattr.__name__, prop)
                if is_map and prop not in to_map.keys():
                    raise KeyError(f"Key: Dict 'to_map' has no key '{prop}' ")
                prop_key = to_map[prop] if is_map else prop
                attr_val = getattr(self, prop)
                prop_dict[prop_key] = (
                    attr_val.name if isinstance(attr_val, Enum) else attr_val
                )
        else:
            prop_dict = self.__to_dict()

        return prop_dict

    def update_dict(self, source: Dict[str, Any], from_map: Dict[str, str] = None):
        """Updates the object with data from a dictionary.

        Args:
            * `source` -- a dictionary with object's properties (name: value) pairs, with to update the object.
                possible values: included in `DictParseable.Props()`.
            * `from_map`(optional) -- a dictionary mapping the source properties to this class properties,
                (src_prop_x: object_prop_x) pairs. use when the source and object properties names are not identical.
                default: None.

        Raises:
            `AttributeError`: if object does not contain one of the property names in `props` or `from_map`.
        """
        is_map = from_map is not None
        for prop, value in source.items():
            is_in_map = is_map and prop in from_map.keys()
            if is_map and not is_in_map:
                logging.warning(
                    f"Key Missing: no such key in 'from_map': '{prop}', using key instead.."
                )
            obj_prop = prop if (not is_map) | (not is_in_map) else from_map[prop]
            if not hasattr(self, obj_prop):
                DictParseable._raiseAttributeError(setattr.__name__, obj_prop)
            setattr(self, obj_prop, value)

    def __to_dict(self) -> Dict[str, Any]:
        """Parses the 'DictParseable' object to a dictionary.

        Returns:
            A dictionary with the object properties, (name: value) pairs"""
        obj_dict: Dict[str, Any] = {}
        for prop in self.__class__.Props():
            attr_val = getattr(self, prop)
            if not isinstance(attr_val, list):
                obj_dict[prop] = (
                    attr_val.name if isinstance(attr_val, Enum) else attr_val
                )
        return obj_dict


# Linear Conversion
class AnalogSensor(DictParseable, Observable):
    # (self,aIn ,aIn_Min = 0, aIn_Max = 512, Val_Min = 0 , Val_Max = 100 ):
    __Properties = namedtuple(
        "__Props",
        "ID TYPE MIN_VALUE MAX_VALUE CURRENT_VAL",
    )

    __Props = __Properties(
        "id",
        "type",
        "min_val",
        "max_val",
        "curr_val",
    )

    @staticmethod
    def Props() -> __Properties:
        """A view on :class:`AnalogSensor` property names"""
        return AnalogSensor.__Props
    
    @property
    def curr_val(self):
        return self.__curr_val

    @curr_val.setter
    def curr_val(self, value):
        old = self.__curr_val
        self.__curr_val = value
        self._notify_change(AnalogSensor.Props().CURRENT_VAL,old,value)

    def __init__(
        self,
        _type: SensorType = None,
        aIn=0,
        aIn_Min=1140,
        aIn_Max=3100,
        min_val=0,
        max_val=100,
        ScalingErrorOffset=3,
    ):
        super().__init__()
        self.__id: str = ""
        self.__type: SensorType = _type
        self.__curr_val: float = 0
        self.__min_val: float = min_val
        self.__max_val: float = max_val

        self.aIn = aIn
        self.aIn_Min = aIn_Min
        self.aIn_Max = aIn_Max
        self.ScalingErrorOffset = ScalingErrorOffset

    def LinearConversion(self, aIn):
        if self.aIn_Max - self.aIn_Min != 0:
            value = ((self.max_val - self.min_val) / (self.aIn_Max - self.aIn_Min)) * (
                aIn - self.aIn_Max
            ) + self.max_val
            self.curr_val = round((100 - value), 1)
            if (self.curr_val > 100) and self.curr_val < (
                self.max_val + self.ScalingErrorOffset
            ):
                self.curr_val = self.max_val
            elif (self.curr_val < 0) and self.curr_val > (
                self.min_val - self.ScalingErrorOffset
            ):
                self.curr_val = self.min_val
            elif self.curr_val < (
                self.min_val - self.ScalingErrorOffset
            ) or self.curr_val > (self.max_val + self.ScalingErrorOffset):
                self.curr_val = -1
        else:
            self.curr_val = -1

        return self.curr_val

    def getCurrentValue(self):
        return self.curr_val
    
    def __str__(self) -> str:
        return f"{self.__type.name} Sensor [{self.__id}]: Max: {self.__max_val}, Min: {self.__min_val}, Current: {self.__curr_val}"

class SensorType(Enum):
    EC = "EC"
    FLOW = "L/s"
    HUMIDITY = "%"
    PH = "pH"
    TEMPERATURE = "C"


class EPModule(DictParseable, Observable):
    __Properties = namedtuple(
        "__Props",
        "ID MAC_ID DESCRIPTION MAX_DURATION DURATION ON_TIME PORT TIMEOUT SENSORS",
    )

    __Props = __Properties(
        "id",
        "mac_id",
        "description",
        "max_duration",
        "duration",
        "on_time",
        "port",
        "timeout",
        "sensors",
    )

    @classmethod
    def Props(cls) -> __Properties:
        """A view on :class:`EPModule` property names"""
        return EPModule.__Props

    # region Properties
    @property
    def id(self) -> str:
        return self.__id

    @id.setter
    def id(self, value: str):
        old = self.__id
        self.__id = value
        self._notify_change(EPModule.Props().ID, old, value)

    @property
    def mac_id(self) -> str:
        return self.__mac_id

    @mac_id.setter
    def mac_id(self, value):
        old = self.__mac_id
        self.__mac_id = value
        self._notify_change(EPModule.Props().MAC_ID, old, value)

    @property
    def description(self):
        return self.__description

    @description.setter
    def description(self, value):
        old = self.__description
        self.__description = value
        self._notify_change(EPModule.Props().DESCRIPTION, old, value)

    @property
    def max_duration(self):
        return self.__max_duration

    @max_duration.setter
    def max_duration(self, value):
        old = self.__max_duration
        self.__max_duration = value
        self._notify_change(EPModule.Props().MAX_DURATION, old, value)

    @property
    def duration(self):
        return self.__duration

    @duration.setter
    def duration(self, value):
        old = self.__duration
        self.__duration = value
        self._notify_change(EPModule.Props().DURATION, old, value)

    @property
    def on_time(self):
        return self.__on_time

    @on_time.setter
    def on_time(self, value):
        old = self.__on_time
        self.__on_time = value
        self._notify_change(EPModule.Props().ON_TIME, old, value)

    @property
    def sensors(self):
        return self.__sensors

    # endregion Properties

    def __init__(
        self,
        mac_id: str = "",
        port: int = 502,
        timeout: float = 1,
        max_duration: int = 600,
    ):
        super().__init__()
        self.__id: str = ""
        self.__mac_id = mac_id
        self.__description: str = ""
        self.__on_time: datetime = datetime.now().astimezone()
        self.__max_duration: int = max_duration
        self.__duration: int = 0
        self.__port: int = port
        self.__timeout: float = timeout
        self.__sensors: List[AnalogSensor] = []
        self.bComError = True
        self.bConnected = False
        self.regs = []

        self.RelayOut = 0
        self.WATER_REQUEST = 0
        self.OUTPUT_MODE = 0
        self.REMAINING_TIME = 0
        self.RelayState = 0
        self.TEMPRATURE = 0
        self.HUMIDITY = 0
        self.CURRENT_WATER_FLOW = 0
        self.TOTAL_WATER_FLOW = 0
        self.RESET_TOTAL_WATER_FLOW = 0

    def add_sensors(self, new_sensors: AnalogSensor | List[AnalogSensor]):
        if isinstance(new_sensors, list):
            self.__sensors.extend(new_sensors)
        else:
            self.__sensors.append(new_sensors)
        self._notify_change(EPModule.Props().SENSORS, None, new_sensors)

    # def get_sensors_values(self) -> List[int]:
    #     values = []
    #     for sensor in self.sensors:
    #         values.append(sensor.curr_val)
    #     return values

    # def SetRelay(self, state):
    #     if self.bConnected:
    #         self.client.write_single_register(3, state)
    #         return True
    #     else:
    #         return False

    # def SetDuration(self, Duration):
    #     if self.bConnected and Duration > 0 and self.duration <= self.max_duration:
    #         self.client.write_single_register(4, Duration)
    #         return True
    #     else:
    #         return False

    # def GetComState(self):
    #     print(str(self.bComError))
    #     return self.bComError

    def __str__(self) -> str:
        return "[Valve: #{0}]: {1}, Max: {2}s, Last on: {3} at {4} for {5}s".format(
            self.mac_id,
            self.description,
            self.__max_duration,
            self.__on_time.strftime("%x"),
            self.__on_time.strftime("%X"),
            self.__duration,
        )
