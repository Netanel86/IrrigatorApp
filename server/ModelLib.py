# PLANTOS Modules Modbus TCP Communication Lib

from pyModbusTCP.client import ModbusClient

import datetime
from typing import Any, Dict, List


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
    PROP_MAX_DURATION = "maxDuration"
    PROP_DURATION = "duration"
    PROP_ON_TIME = "onTime"
    PROP_COM_ERROR = ""

    def __init__(
        self, IP="", port=502, timeout=0.5, max_duration: int = 600
    ):
        self.id = ""
        self.description = ""
        self.on_time = datetime.datetime.now().astimezone()
        self.max_duration = max_duration
        self.Duration = 0
        self.IP = IP

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
        self.client.host(self.IP)
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
                self.Duration = self.regs[4]
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
        if self.bConnected and Duration > 0 and self.Duration <= self.MaxDuration:
            self.client.write_single_register(4, Duration)
            return True
        else:
            return False

    def GetComState(self):
        print(str(self.bComError))
        return self.bComError

    def __str__(self) -> str:
        return "[Valve: #{0}]: {1}, Max: {2}s, Last on: {3} at {4} for {5}s".format(
            self.index,
            self.description,
            self.max_duration,
            self.on_time.strftime("%x"),
            self.on_time.strftime("%X"),
            self.duration,
        )

    def to_dict(self) -> Dict[str, Any]:
        """Parses :class:`Valve` to a dictionary.\n
        Returns:
            A dictionary with this :class:`Valve` propeties, name and value pairs"""
        return {
            self.PROP_IP: self.IP,
            self.PROP_DESCRIPTION: self.description,
            self.PROP_MAX_DURATION: self.max_duration,
            self.PROP_DURATION: self.Duration,
            self.PROP_ON_TIME: self.on_time,
        }

    def to_prop_dict(self, props: List[str]) -> Dict[str, Any]:
        """Parses only the specified :class:`Valve` properties to a dictionary.\n
        Args:
            props -- a list of properties names to parse\n
        Returns:
            A dictionary with the specified propeties, name and value pairs\n
        Remarks:
            use the :class:`Valve` constant property name fields:
                :field:`Valve.PROP_IP`\n
                :field:`Valve.PROP_DESCRIPTION`\n
                :field:`Valve.PROP_MAX_DURATION`\n
                :field:`Valve.PROP_DURATION`\n
                :field:`Valve.PROP_ON_TIME`
        """
        prop_dict = {}

        for prop in props:
            match prop:
                case EPModule.PROP_IP:
                    prop_dict[prop] = self.IP
                case EPModule.PROP_DESCRIPTION:
                    prop_dict[prop] = self.description
                case EPModule.PROP_MAX_DURATION:
                    prop_dict[prop] = self.max_duration
                case EPModule.PROP_DURATION:
                    prop_dict[prop] = self.Duration
                case EPModule.PROP_ON_TIME:
                    prop_dict[prop] = self.on_time
        return prop_dict

    @staticmethod
    def from_dict(module_id: str, source: Dict[str, Any]):
        """Parses and creates a new :class:`Valve` from a dictionary.\n
        Args:
            valve_id -- the valve's database id\n
            source -- a dictionary with object's properties, name and value pairs\n
        Returns:
            :class:`Valve` -- a Valve initialized with dictionary data\n
        Remarks:
            use the :class:`Valve` constant property name fields:
                :field:`Valve.PROP_IP`\n
                :field:`Valve.PROP_DESCRIPTION`\n
                :field:`Valve.PROP_MAX_DURATION`\n
                :field:`Valve.PROP_DURATION`\n
                :field:`Valve.PROP_ON_TIME`
        """
        module = EPModule(
            source[EPModule.PROP_IP], max_duration=source[EPModule.PROP_MAX_DURATION]
        )
        module.id = module_id
        module.Duration = source[EPModule.PROP_DURATION]
        module.on_time = source[EPModule.PROP_ON_TIME]
        module.description = source[EPModule.PROP_DESCRIPTION]

        return module
