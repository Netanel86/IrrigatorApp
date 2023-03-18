from __future__ import annotations
from typing import List
from collections import namedtuple
from enum import Enum
from datetime import datetime
from infra import *

# TODO move SensorType inside AnalogSensor, rename to Type
# TODO rename AnalogSensor to Sensor
class SensorType(Enum):
    NONE = ""
    EC = "EC"
    FLOW = "L/s"
    HUMIDITY = "%"
    PH = "pH"
    TEMPERATURE = "C"


class AnalogSensor(IDable, DictParseable, Observable):
    # (self,aIn ,aIn_Min = 0, aIn_Max = 512, Val_Min = 0 , Val_Max = 100 ):
    __Properties = namedtuple(
        "__Props",
        "ID DEVICE_ID TYPE MIN_VALUE MAX_VALUE CURRENT_VAL",
    )

    __Props = __Properties(
        "id",
        "device_id",
        "type",
        "min_val",
        "max_val",
        "curr_val",
    )

    @staticmethod
    def Props() -> __Properties:
        """A view on :class:`AnalogSensor` property names"""
        return AnalogSensor.__Props

    # region Properties
    @property
    def id(self) -> int:
        return self.__id

    @id.setter
    def id(self, value: int):
        old = self.__id
        self.__id = value
        self._notify_change(AnalogSensor.Props().ID, old, value)

    @property
    def type(self) -> SensorType:
        return self.__type

    @type.setter
    def type(self, value: SensorType):
        old = self.__type
        self.__type = value
        self._notify_change(AnalogSensor.Props().TYPE, old, value)

    @property
    def min_val(self) -> float:
        return self.__min_val

    @min_val.setter
    def min_val(self, value: float):
        old = self.__min_val
        self.__min_val = value
        self._notify_change(AnalogSensor.Props().MIN_VALUE, old, value)

    @property
    def max_val(self) -> float:
        return self.__max_val

    @max_val.setter
    def max_val(self, value: float):
        old = self.__max_val
        self.__max_val = value
        self._notify_change(AnalogSensor.Props().MAX_VALUE, old, value)

    @property
    def curr_val(self) -> float:
        return self.__curr_val

    @curr_val.setter
    def curr_val(self, value: float):
        old = self.__curr_val
        self.__curr_val = value
        self._notify_change(AnalogSensor.Props().CURRENT_VAL, old, value)

    @property
    def device_id(self) -> str:
        return self._device_id

    @device_id.setter
    def device_id(self, value: str):
        old = self._device_id
        self._device_id = value
        self._notify_change(AnalogSensor.Props().DEVICE_ID, old, value)

    # endregion Properties

    def __init__(
        self,
        _type: SensorType = SensorType.NONE,
        device_id: str = "",
        aIn=0,
        aIn_Min=1140,
        aIn_Max=3100,
        min_val=0.0,
        max_val=100.0,
        ScalingErrorOffset=3,
    ):
        super().__init__()
        self.__id: int = None
        self._device_id: str = device_id
        self.__type: SensorType = _type
        self.__curr_val: float = 0.0
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

    def __str__(self) -> str:
        return f"{self.__type.name} Sensor [{self.__id}]: Max: {self.__max_val}, Min: {self.__min_val}, Current: {self.__curr_val}"


class EPModule(IDable, DictParseable, Observable):
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
    def id(self) -> int:
        return self._id

    @id.setter
    def id(self, value: int):
        old = self._id
        self._id = value
        self._notify_change(EPModule.Props().ID, old, value)

    @property
    def mac_id(self) -> str:
        return self._mac_id

    @mac_id.setter
    def mac_id(self, value):
        old = self._mac_id
        self._mac_id = value
        self._notify_change(EPModule.Props().MAC_ID, old, value)

    @property
    def description(self):
        return self._description

    @description.setter
    def description(self, value):
        old = self._description
        self._description = value
        self._notify_change(EPModule.Props().DESCRIPTION, old, value)

    @property
    def max_duration(self) -> int:
        return self._max_duration

    @max_duration.setter
    def max_duration(self, value):
        old = self._max_duration
        self._max_duration = value
        self._notify_change(EPModule.Props().MAX_DURATION, old, value)

    @property
    def duration(self) -> int:
        return self._duration

    @duration.setter
    def duration(self, value):
        old = self._duration
        self._duration = value
        self._notify_change(EPModule.Props().DURATION, old, value)

    @property
    def on_time(self):
        return self._on_time

    @on_time.setter
    def on_time(self, value):
        old = self._on_time
        self._on_time = value
        self._notify_change(EPModule.Props().ON_TIME, old, value)

    @property
    def sensors(self) -> List[AnalogSensor]:
        return list(self._sensors.values())

    # endregion Properties

    def __init__(
        self,
        mac_id: str = "",
        port: int = 502,
        timeout: float = 1,
        max_duration: int = 600,
    ):
        super().__init__()
        self._id: int = None
        self._mac_id = mac_id
        self._description: str = ""
        self._on_time: datetime = datetime.now().astimezone()
        self._max_duration: int = max_duration
        self._duration: int = 0
        self.port: int = port
        self.timeout: float = timeout
        self._sensors: Dict[str, AnalogSensor] = {}
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
            self._sensors.update({sensor.device_id: sensor for sensor in new_sensors})
        else:
            self._sensors[new_sensors.device_id] = new_sensors

        self._notify_change(EPModule.Props().SENSORS, None, new_sensors)

    def get_sensor(self, device_id: str) -> AnalogSensor:
        return self._sensors[device_id]

    def __str__(self) -> str:
        return "[Valve: #{0}]: {1}, Max: {2}s, Last on: {3} at {4} for {5}s".format(
            self.mac_id,
            self.description,
            self._max_duration,
            self._on_time.strftime("%x"),
            self._on_time.strftime("%X"),
            self._duration,
        )


class Command(DictParseable):
    class Actions(Enum):
        REFRESH = 0
        OPEN = 1
        CLOSE = 2
        UPDATE = 3

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
        self.id = id
        self.timestamp = time.astimezone()
        self.action = action
        self.attributes = attr

    def __str__(self) -> str:
        to_string = f"[{self.timestamp.strftime('%Y-%m-%d %X')}: Command]:"

        match self.action:
            case Command.Actions.OPEN:
                to_string += f" Turn ON Valve #{self.attributes[Command.Attrs.IP]} for: {self.attributes[Command.Attrs.DURATION]}s"

            case Command.Actions.CLOSE:
                to_string += f" Turn OFF Valve #{self.attributes[Command.Attrs.IP]}"

            case Command.Actions.UPDATE:
                to_string += f" Edit Valve #{self.attributes[Command.Attrs.IP]} description to: {self.attributes[Command.Attrs.DESCRIPTION]}"

        return to_string
