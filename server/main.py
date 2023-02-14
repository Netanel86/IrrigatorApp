from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os, subprocess
from typing import Dict, List, Callable
from gui import GUI
from model import *
from repository import Repository
from connections.mqtt import MQTTConnection

# check if execute from SSH and run GUI on the remote Device
if os.environ.get("DISPLAY", "") == "":
    print("no display found. Using :0.0")
    os.environ.__setitem__("DISPLAY", ":0.0")

# Global Parameters
running = True
bFirstCycle = True
GlobalCounter = 0


# "78:21:84:8C:AF:FC"

# def on_close():
#     global running
#     running = False
# Gui = GUI()
# Gui.set_on_close_callback(on_close)
# for m_idx, module in enumerate(modules.values()):
#     Gui.add_button(str(m_idx), lambda: module.SetRelay(not module.RelayOut))
#     if len(module.sensors) != 0:
#         for s_idx, sensor in enumerate(module.sensors):
#             Gui.add_label("{}_{}".format(m_idx, s_idx), "Module {}".format(m_idx))


mqttModuleFields = namedtuple(
    "mqttModuleFields",
    "KEY MAC_ID MAX_DURATION DURATION ON_TIME TIMEOUT",
)
MQTT_MODULE_FIELDS = mqttModuleFields(
    "module",
    "mac_id",
    "max_duration",
    "duration",
    "on_time",
    "timeout",
)

mqttSensorFields = namedtuple(
    "mqttSensorFields",
    "KEY DEVICE_ID TYPE MIN_VALUE MAX_VALUE CURRENT_VAL",
)
MQTT_SENSOR_FIELDS = mqttSensorFields(
    "sensors",
    "device_id",
    "type",
    "min_val",
    "max_val",
    "curr_val",
)

mqttTopics = namedtuple("mqttTopics", "NEW_DEVICE")
MQTT_TOPICS = mqttTopics("connected_devices")


class Mqtt(NamedTuple):
    MODULE: mqttModuleFields
    SENSORS: mqttSensorFields
    TOPICS: mqttTopics


MQTT = Mqtt(MQTT_MODULE_FIELDS, MQTT_SENSOR_FIELDS, MQTT_TOPICS)
MODULE_MAP_FROM_MQTT = {
    MQTT.MODULE.MAC_ID: EPModule.Props().MAC_ID,
    MQTT.MODULE.MAX_DURATION: EPModule.Props().MAX_DURATION,
    MQTT.MODULE.DURATION: EPModule.Props().DURATION,
    MQTT.MODULE.ON_TIME: EPModule.Props().ON_TIME,
    MQTT.MODULE.TIMEOUT: EPModule.Props().TIMEOUT,
}
SENSOR_MAP_FROM_MQTT = {
    MQTT.SENSORS.TYPE: AnalogSensor.Props().TYPE,
    MQTT.SENSORS.MAX_VALUE: AnalogSensor.Props().MAX_VALUE,
    MQTT.SENSORS.MIN_VALUE: AnalogSensor.Props().MIN_VALUE,
    MQTT.SENSORS.CURRENT_VAL: AnalogSensor.Props().CURRENT_VAL,
}


class System(object):
    def __init__(self) -> None:
        logging.setLoggerClass(Logger)
        self.logger = logging.getLogger(self.__class__.__name__)
        self.repo = Repository()
        self.modules = self.repo.get_modules()
        self.__init_mqtt()

    def __init_mqtt(self):
        self.__init_background_broker()
        self.client = MQTTConnection("PLANTOS")
        self.client.connect("192.168.1.177", 1883)

        self.client.subscribe(MQTT.TOPICS.NEW_DEVICE, self.connected_devices)

        for module in self.modules.values():
            self.client.subscribe(module.mac_id, self.update_module)

    def __init_background_broker(self):
        enviroments = ["ProgramFiles", "ProgramFiles(x86)"]
        file_path = "mosquitto\mosquitto.exe"

        for envirom in enviroments:
            broker_path = os.path.join(os.environ[envirom], file_path)
            if os.path.exists(broker_path):
                break

        if not os.path.exists(broker_path):
            raise FileNotFoundError(
                f"Failed to run broker, file '{file_path}' not found in any of the given enviroments: {enviroments}."
            )

        subprocess.Popen(broker_path)
        method_sig = self.__init_background_broker.__name__.removeprefix("__")
        self.logger.info(
            f"{method_sig}> background broker '{os.path.split(broker_path)[1]}' started succefully"
        )

    def connected_devices(self, data: Dict[str, List | Dict]):
        module = self.modules.get(data[MQTT.MODULE.KEY][MQTT.MODULE.MAC_ID], None)
        if module is None:
            self.create_module(data)
        else:
            self.update_module(data)

    def create_module(self, data: Dict[str, List | Dict]):
        module: EPModule = EPModule.from_dict(
            data[MQTT.MODULE.KEY], MODULE_MAP_FROM_MQTT
        )
        
        sensors: List[AnalogSensor] = []
        for sensor_dict in data[MQTT.SENSORS.KEY]:
            sensors.append(AnalogSensor.from_dict(sensor_dict, SENSOR_MAP_FROM_MQTT))
        module.add_sensors(sensors)
        
        self.repo.add_module(module)
        self.client.subscribe(module.mac_id, self.update_module)

    def update_module(self, data: Dict[str, List | Dict]):
        module = self.modules[data[MQTT.MODULE.KEY][MQTT.MODULE.MAC_ID]]
        module.update_dict(data[MQTT.MODULE.KEY], MODULE_MAP_FROM_MQTT)
        
        for sensor_dict in data[MQTT.SENSORS.KEY]:
            module.get_sensor(sensor_dict[MQTT.SENSORS.DEVICE_ID]).update_dict(
                sensor_dict, SENSOR_MAP_FROM_MQTT
            )


sys = System()
input()
# prgMain Thread
# prg_Main = threading.Thread(target=prgMain)

# RUN PROGRAM
# if __name__ == "__main__":
# RUN prgMain
# prg_Main.start()
# RUN GUI
# Gui.run()
# gui.mainloop()
