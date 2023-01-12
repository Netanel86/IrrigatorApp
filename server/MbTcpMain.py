from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os
import logging
from typing import Dict, List, Callable
from gui import GUI
from ModelLib import *
import paho.mqtt.client as mqtt

# from repository import Repository

# check if execute from SSH and run GUI on the remote Device
if os.environ.get("DISPLAY", "") == "":
    print("no display found. Using :0.0")
    os.environ.__setitem__("DISPLAY", ":0.0")

# Global Parameters
running = True
bFirstCycle = True
GlobalCounter = 0

# repo = Repository()"78:21:84:8C:AF:FC"
# modules: Dict[str, EPModule] = repo.get_modules()

module_1 = EPModule("esp32")
module_1.description = "PY#0"
module_1.sensors.append(AnalogSensor(SensorType.HUMIDITY))


class ClientManager:
    def __init__(self, modules: List[EPModule]) -> None:
        self.clients: Dict[str, mqtt.Client] = {}
        self.__init_clients(modules)

    def add_client(
        self, name, mac_id, topic, callback: Callable[[Dict[str, Any]], None]
    ):
        client = self.clients[mac_id] = mqtt.Client(name)

        def on_message(client, data, msg):
            print(msg.payload.decode())
            # use a json parser to parse msg.payload to dictionary
            callback(msg)

        def on_connect(client: mqtt.Client, userdata, flags, rc):
            logging.info(f"Connected with result code {rc}")
            client.subscribe(topic)

        client.on_connect = on_connect
        client.on_message = on_message

    def __init_clients(self, modules: List[EPModule]):
        for module in modules:
            client = self.clients[module.ip] = mqtt.Client(module.description)

            def on_message(client, data, msg):
                print(msg.payload.decode())

            def on_connect(client: mqtt.Client, userdata, flags, rc):
                logging.info(f"Connected with result code {rc}")
                client.subscribe(module.ip)

            client.on_connect = on_connect
            client.on_message = on_message

    def connect_clients(self):
        for client in self.clients.values():
            client.connect(module.ip, 1883)
            client.loop_start()


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


def prgMain():
    client = connect_mqtt()
    client.loop_forever()


# prgMain Thread
prg_Main = threading.Thread(target=prgMain)

# RUN PROGRAM
if __name__ == "__main__":
    # RUN prgMain
    prg_Main.start()
    # RUN GUI
    # Gui.run()
    # gui.mainloop()
