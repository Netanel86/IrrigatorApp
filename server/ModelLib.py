# PLANTOS Modules Modbus TCP Communication Lib
from __future__ import annotations
from pyModbusTCP.client import ModbusClient

from datetime import datetime
from typing import Any, Dict, List, Tuple

# Linear Conversion
class AnalogSensor(
    object
):  # (self,aIn ,aIn_Min = 0, aIn_Max = 512, Val_Min = 0 , Val_Max = 100 ):
    def __init__(
        self,
        aIn=0,
        aIn_Min=1140,
        aIn_Max=3100,
        Val_Min=0,
        Val_Max=100,
        ScalingErrorOffset=3,
    ):
        self.aIn = aIn
        self.aIn_Min = aIn_Min
        self.aIn_Max = aIn_Max
        self.Val_Min = Val_Min
        self.Val_Max = Val_Max
        self.ScalingErrorOffset = ScalingErrorOffset
        self.SensorValue = 0

    def LinearConversion(self, aIn):
        if self.aIn_Max - self.aIn_Min != 0:
            value = ((self.Val_Max - self.Val_Min) / (self.aIn_Max - self.aIn_Min)) * (
                aIn - self.aIn_Max
            ) + self.Val_Max
            self.SensorValue = round((100 - value), 1)
            if (self.SensorValue > 100) and self.SensorValue < (
                self.Val_Max + self.ScalingErrorOffset
            ):
                self.SensorValue = self.Val_Max
            elif (self.SensorValue < 0) and self.SensorValue > (
                self.Val_Min - self.ScalingErrorOffset
            ):
                self.SensorValue = self.Val_Min
            elif self.SensorValue < (
                self.Val_Min - self.ScalingErrorOffset
            ) or self.SensorValue > (self.Val_Max + self.ScalingErrorOffset):
                self.SensorValue = -1
        else:
            self.SensorValue = -1

        return self.SensorValue

    def GetSensorValue(self):
        return self.SensorValue


class EPModule(object):
    PROP_ID = "id"
    PROP_IP = "ip"
    PROP_DESCRIPTION = "description"
    PROP_MAX_DURATION = "max_duration"
    PROP_DURATION = "duration"
    PROP_ON_TIME = "on_time"
    PROP_PORT = "port"
    PROP_TIMEOUT = "timeout"

    def __init__(
        self,
        ip: str = "",
        port: int = 502,
        timeout: float = 0.5,
        max_duration: int = 600,
    ):
        self.id: str = ""
        self.description: str = ""
        self.on_time: datetime = datetime.now().astimezone()
        self.max_duration: int = max_duration
        self.duration: int = 0
        self.ip: str = ip
        self.port = port
        self.timeout = timeout

        self.client = ModbusClient()
        self.bComError = True
        self.bConnected = False
        self.regs = []
        self.SoilSensor1 = AnalogSensor()
        self.SoilSensor2 = AnalogSensor()
        self.SoilSensor3 = AnalogSensor()
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
        self.client.host(self.ip)
        self.client.port(self.port)
        self.client.timeout(self.timeout)
        if not self.client.is_open():
            if not self.client.open():
                self.bcomError = True
                self.bConnected = False
        else:
            self.bcomError = False
            self.bConnected = True

    def ReadClentRegs(self):
        if self.client.is_open():
            self.regs = self.client.read_holding_registers(0, 15)
            if self.regs:
                # self.bcomError = False
                # self.bConnected = True
                self.SoilSensor1.LinearConversion(self.regs[0])
                self.SoilSensor2.LinearConversion(self.regs[1])
                self.SoilSensor3.LinearConversion(self.regs[2])
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

    def GetSensors(self):
        Sensors = list
        return [
            self.SoilSensor1.GetSensorValue(),
            self.SoilSensor2.GetSensorValue(),
            self.SoilSensor3.GetSensorValue(),
        ]

    def SetRelay(self, state):
        if self.bConnected:
            self.client.write_single_register(3, state)
            return True
        else:
            return False

    def SetDuration(self, Duration):
        if self.bConnected and Duration > 0 and self.duration <= self.MaxDuration:
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

    def __to_dict(self) -> Dict[str, Any]:
        """Parses the module to a dictionary.\n
        Returns:
            A dictionary with the module properties, name and value pairs"""
        return {
            EPModule.PROP_ID: self.id,
            EPModule.PROP_IP: self.ip,
            EPModule.PROP_DESCRIPTION: self.description,
            EPModule.PROP_MAX_DURATION: self.max_duration,
            EPModule.PROP_DURATION: self.duration,
            EPModule.PROP_ON_TIME: self.on_time,
            EPModule.PROP_PORT: self.port,
            EPModule.PROP_TIMEOUT: self.timeout,
        }

    def to_dict(self, props: List[str] = None) -> Dict[str, Any]:
        """Parses the module to a dictionary.

        Args:
            props(optional) -- a list of properties names to parse, if value is set only the specified properties would be parsed,
                possible values: `EPModule.PROP_ID`, `EPModule.PROP_IP`, `EPModule.PROP_DESCRIPTION`,
                `EPModule.PROP_MAX_DURATION`, `EPModule.PROP_DURATION`, `EPModule.PROP_ON_TIME`,
                `EPModule.PROP_PORT` and `EPModule.PROP_TIMEOUT`.

        Returns:
            A dictionary with the specified propeties, name and value pairs
        """
        prop_dict = {}

        if props is not None:
            for prop in props:
                prop_dict[prop] = self.__getattr(prop)
        else:
            prop_dict = self.__to_dict()

        return prop_dict

    def __to_tuple(self) -> Tuple:
        return (
            self.id,
            self.ip,
            self.description,
            self.max_duration,
            self.duration,
            self.on_time,
            self.port,
            self.timeout,
        )

    def to_tuple(self, props: List[str] = None) -> Tuple:
        if props is not None:
            to_tup = ()
            for prop_name in props:
                to_tup += (self.__getattr(prop_name),)
        else:
            to_tup = self.__to_tuple()

        return to_tup

    @staticmethod
    def from_tuple(source: Tuple[Tuple]) -> EPModule:
        IDX_PROP = 0
        IDX_VAL = 1
        module = EPModule()
        for item in source:
            module.__setattr(item[IDX_PROP], item[IDX_VAL])

        return module

    @staticmethod
    def from_dict(source: Dict[str, Any]) -> EPModule:
        """Parses and creates a new module from a dictionary.

        Args:
            source: a dictionary with module's properties, name and value pairs
                possible name values: `EPModule.PROP_ID`, `EPModule.PROP_IP`, `EPModule.PROP_DESCRIPTION`,
                `EPModule.PROP_MAX_DURATION`, `EPModule.PROP_DURATION`, `EPModule.PROP_ON_TIME`,
                `EPModule.PROP_PORT` and `EPModule.PROP_TIMEOUT`.

        Returns:
            `EPModule`: a module initialized with dictionary data
        """
        module = EPModule()
        for name, value in source.items():
            module.__setattr(name, value)

        return module

    def __getattr(self, name: str) -> Any:
        attr = None
        match name:
            case EPModule.PROP_ID:
                attr = self.id
            case EPModule.PROP_IP:
                attr = self.ip
            case EPModule.PROP_DESCRIPTION:
                attr = self.description
            case EPModule.PROP_MAX_DURATION:
                attr = self.max_duration
            case EPModule.PROP_DURATION:
                attr = self.duration
            case EPModule.PROP_ON_TIME:
                attr = self.on_time
            case EPModule.PROP_PORT:
                attr = self.port
            case EPModule.PROP_TIMEOUT:
                attr = self.timeout
            case _:
                raise AttributeError(
                    "'{_class}.{_method}()': Attribute '{_property}' not found in class '{_class}'".format(
                        _method=self.__getattr.__name__,
                        _property=name,
                        _class=self.__class__.__name__,
                    )
                )
        return attr

    def __setattr(self, name: str, value: Any) -> None:
        match name:
            case EPModule.PROP_ID:
                self.id = value
            case EPModule.PROP_IP:
                self.ip = value
            case EPModule.PROP_DESCRIPTION:
                self.description = value
            case EPModule.PROP_MAX_DURATION:
                self.max_duration = value
            case EPModule.PROP_DURATION:
                self.duration = value
            case EPModule.PROP_ON_TIME:
                self.on_time = value
            case EPModule.PROP_PORT:
                self.port = value
            case EPModule.PROP_TIMEOUT:
                self.timeout = value
            case _:
                raise AttributeError(
                    "'{_class}.{_method}()': Attribute '{_property}' not found in class '{_class}', or is unchangable".format(
                        _method=self.__setattr.__name__,
                        _property=name,
                        _class=self.__class__.__name__,
                    )
                )
