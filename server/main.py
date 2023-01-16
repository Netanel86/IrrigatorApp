from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os
from typing import Dict, List, Callable
from gui import GUI
from model import *
from repository import Repository
from mqtt_manager import MQTTManager

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

repo = Repository()


def connected_devices(data: Dict[str, Any]):
    modules = repo.get_modules()
    for val in data.values():
        module = modules.get(val, None)
        if module is None:
            repo.add_module(EPModule(val))


def prgMain():
    modules: Dict[str, EPModule] = repo.get_modules()

    client = MQTTManager("RaspberryPi")
    client.connect("192.168.1.177", 1883)

    client.subscribe("connected_devices", connected_devices)
    for module in modules.values():
        client.subscribe(module.mac_id, lambda dict: module.update_dict(dict))


prgMain()

# prgMain Thread
# prg_Main = threading.Thread(target=prgMain)

# RUN PROGRAM
# if __name__ == "__main__":
# RUN prgMain
# prg_Main.start()
# RUN GUI
# Gui.run()
# gui.mainloop()
