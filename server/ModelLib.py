# PLANTOS Modules Modbus TCP Communication Lib
from __future__ import annotations
from abc import ABC
from collections import namedtuple
from enum import Enum
import logging
from pyModbusTCP.client import ModbusClient
from datetime import datetime
from typing import Any, Dict, List, NamedTuple, Tuple


class DictParseable(ABC):
    @property
    def id(self) -> str:
        return self._id

    @id.setter
    def id(self, value):
        self._id = value

    @staticmethod
    def Props() -> NamedTuple:
        """An abstract representation of object's property names.

        To be implemented and initialized with object property names.
        """
        raise NotImplementedError(
            "property getter not implemented in class '{}': '{}'".format(
                "cls.__class__.__name__", "Props"
            )
        )

    def __to_dict(self) -> Dict[str, Any]:
        """Parses the 'DictParseable' object to a dictionary.

        Returns:
            A dictionary with the object properties, (name: value) pairs"""
        obj_dict: Dict[str, Any] = {}
        for prop in self.Props:
            attr_val = getattr(self, prop)
            if not isinstance(attr_val, list):
                obj_dict[prop] = (
                    attr_val.name if isinstance(attr_val, Enum) else attr_val
                )
        return obj_dict

    def to_dict(
        self, props: Tuple[str] = None, to_map: Dict[str, str] = None
    ) -> Dict[str, Any]:
        """Parses the 'DictParseable' object to a dictionary.

        Args:
            props(optional) -- a list of properties names to parse, if set only the specified properties would be parsed,
                possible values: included in `DictParseable.Props()`.

        Returns:
            A dictionary with the object's specified properties, (name: value) pairs
        """
        prop_dict = {}
        is_map = to_map is not None
        is_props = props is not None

        collection = props if is_props else to_map.keys() if is_map else None

        if collection is not None:
            for prop in collection:
                if not hasattr(self, prop):
                    self._raiseAttributeError(getattr.__name__, prop)
                if is_map and prop not in to_map.keys():
                    raise KeyError("Key: Dict 'to_map' has no key '{}' ".format(prop))
                prop_key = to_map[prop] if is_map else prop
                attr_val = getattr(self, prop)
                prop_dict[prop_key] = (
                    attr_val.name if isinstance(attr_val, Enum) else attr_val
                )
        else:
            prop_dict = self.__to_dict()

        return prop_dict

    @classmethod
    def from_dict(
        cls, source: Dict[str, Any], from_map: Dict[str, str] = None
    ) -> DictParseable:
        """Parses and creates a new object from a dictionary.

        Args:
            source: a dictionary with object's properties, (name: value) pairs.
                possible values: included in `DictParseable.Props()`.

        Returns:
            `DictParseable`: the child object derived from `DictParseable`, initialized with dictionary data.
        """
        module = cls()
        is_map = from_map is not None
        for prop, value in source.items():
            is_in_map = is_map and prop in from_map.keys()
            obj_prop = prop if not is_map | (not is_in_map) else from_map[prop]
            if is_map and not is_in_map:
                logging.warning(
                    "Key Missing: no such key in 'from_map': '{}', using key instead..".format(
                        prop
                    )
                )
            if not hasattr(module, obj_prop):
                cls._raiseAttributeError(setattr.__name__, obj_prop)
            setattr(module, obj_prop, value)

        return module

    @classmethod
    def _raiseAttributeError(cls, method_name, prop_name):
        raise AttributeError(
            "'{_class}.{_method}()': no such attribute: '{_property}'".format(
                _method=method_name,
                _property=prop_name,
                _class=cls.__name__,
            )
        )


# Linear Conversion
class AnalogSensor(DictParseable):
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
        self._id: str = ""
        self.type: SensorType = _type
        self.curr_val: float = 0
        self.min_val: float = min_val
        self.max_val: float = max_val

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


class SensorType(Enum):
    EC = "EC"
    FLOW = "L/s"
    HUMIDITY = "%"
    PH = "pH"
    TEMPERATURE = "C"


class EPModule(DictParseable):
    __Properties = namedtuple(
        "__Props",
        "ID MASTER_ID IP DESCRIPTION MAX_DURATION DURATION ON_TIME PORT TIMEOUT SENSORS",
    )

    __Props = __Properties(
        "id",
        "master_id",
        "ip",
        "description",
        "max_duration",
        "duration",
        "on_time",
        "port",
        "timeout",
        "sensors",
    )

    @staticmethod
    def Props() -> __Properties:
        """A view on :class:`EPModule` property names"""
        return EPModule.__Props

    def __init__(
        self,
        ip: str = "",
        port: int = 502,
        timeout: float = 1,
        max_duration: int = 600,
    ):
        self._id: str = ""
        self.master_id: str = ""
        self.description: str = ""
        self.on_time: datetime = datetime.now().astimezone()
        self.max_duration: int = max_duration
        self.duration: int = 0
        self.ip = ip
        self.port: int = port
        self.timeout: float = timeout
        self.sensors: List[AnalogSensor] = []

        self.client = ModbusClient(host=self.ip, port=self.port, timeout=self.timeout)
        self.bComError = True
        self.bConnected = False
        self.regs = []
        # self.SoilSensor1 = AnalogSensor()
        # self.SoilSensor2 = AnalogSensor()
        # self.SoilSensor3 = AnalogSensor()
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

    def connect(self):
        # self.client.host("192.168.0.201")
        # self.client.port(self.port)
        # self.client.timeout(self.timeout)
        if not self.client.is_open:
            if not self.client.open():
                self.bcomError = True
                self.bConnected = False
        else:
            self.bcomError = False
            self.bConnected = True

    def ReadClentRegs(self):
        if self.client.is_open:
            self.regs = self.client.read_holding_registers(0, 15)
            if self.regs:
                # self.bcomError = False
                # self.bConnected = True
                for idx, sensor in enumerate(self.sensors):
                    sensor.LinearConversion(self.regs[idx])
                # self.SoilSensor1.LinearConversion(self.regs[0])
                # self.SoilSensor2.LinearConversion(self.regs[1])
                # self.SoilSensor3.LinearConversion(self.regs[2])
                self.RelayOut = self.regs[3]
                self.duration = self.regs[4]
                self.WATER_REQUEST = self.regs[5]
                self.OUTPUT_MODE = self.regs[6]
                self.REMAINING_TIME = self.regs[7]
                self.RelayState = self.regs[8]
                self.TEMPRATURE = self.regs[9]
                self.HUMIDITY = self.regs[10]
                self.CURRENT_WATER_FLOW = self.regs[11]
                self.TOTAL_WATER_FLOW = self.regs[12]
                self.RESET_TOTAL_WATER_FLOW = self.regs[13]
        else:
            self.bcomError = True
            self.bConnected = False

    def GetRegs(self):
        if self.regs:
            return self.regs
        else:
            return 0

    def get_sensors_values(self) -> List[int]:
        values = []
        for sensor in self.sensors:
            values.append(sensor.curr_val)
        return values

    def SetRelay(self, state):
        if self.bConnected:
            self.client.write_single_register(3, state)
            return True
        else:
            return False

    def SetDuration(self, Duration):
        if self.bConnected and Duration > 0 and self.duration <= self.max_duration:
            self.client.write_single_register(4, Duration)
            return True
        else:
            return False

    def GetComState(self):
        print(str(self.bComError))
        return self.bComError

    def __str__(self) -> str:
        return "[Valve: #{0}]: {1}, Max: {2}s, Last on: {3} at {4} for {5}s".format(
            self.ip,
            self.description,
            self.max_duration,
            self.on_time.strftime("%x"),
            self.on_time.strftime("%X"),
            self.duration,
        )
