from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os
from typing import Dict
from gui import GUI
from ModelLib import EPModule, AnalogSensor, SensorType

# from repository import Repository
from paho.mqtt import client as mqtt_client

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


def on_connect(client, userdata, flags, rc):
    # This will be called once the client connects
    print(f"Connected with result code {rc}")
    # Subscribe here!
    module_1.connect(client)


def connect_mqtt():
    client = mqtt_client.Client("pi")
    client.on_connect = on_connect
    client.connect("192.168.0.177", 1883)
    return client


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
