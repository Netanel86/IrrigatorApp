from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os
from typing import Dict
from gui import GUI
from ModelLib import EPModule
from repository import Repository
import time

# check if execute from SSH and run GUI on the remote Device
if os.environ.get("DISPLAY", "") == "":
    print("no display found. Using :0.0")
    os.environ.__setitem__("DISPLAY", ":0.0")

# Global Parameters
running = True
bFirstCycle = True
GlobalCounter = 0

repo = Repository()
modules: Dict[str, EPModule] = repo.get_modules()


def on_close():
    global running
    running = False


Gui = GUI()
Gui.set_on_close_callback(on_close)

for m_idx, module in enumerate(modules.values()):
    Gui.add_button(str(m_idx), lambda: module.SetRelay(not module.RelayOut))
    if len(module.sensors) != 0:
        for s_idx, sensor in enumerate(module.sensors):
            Gui.add_label("{}_{}".format(m_idx, s_idx), "Module {}".format(m_idx))


def prgMain():
    while running:
        time.sleep(0.4)
        for m_idx, module in enumerate(modules.values()):
            btn_key = str(m_idx)
            if module.bConnected:
                module.ReadClentRegs()
                if m_idx <= 2:
                    print("Device: {} Connected: {}".format(Device.ip, Device.bConnected))
                if module.RelayOut:
                    buttonText = "ON -> " + str(module.REMAINING_TIME)
                    Gui.buttons[btn_key].config(
                        text=buttonText,
                        fg="black",
                        bg="green",
                        activebackground="green",
                    )
                else:
                    buttonText = "OFF"
                    Gui.buttons[btn_key].config(
                        text=buttonText, fg="white", bg="gray", activebackground="gray"
                    )

                for s_idx in enumerate(module.sensors):
                    lbl_key = btn_key + "_" + str(s_idx)
                    sDeviceText = "Module: {}\nS#{}: {}%".format(
                        module.ip, s_idx, sensor.curr_val
                    )
                    Gui.lables[lbl_key].config(text=sDeviceText)
            else:
                # print("Device " + str(Devices[i].IP) + "  ComError!!!")
                buttonText = "Com Error"
                Gui.buttons[btn_key].config(
                    text=buttonText, fg="black", bg="red", activebackground="red"
                )

        # for i in range(len(module.sensors)):
        #         sDeviceText = "Device " + str(modules[i].ip) + "\n S1: " + str(modules[i].SoilSensor1.SensorValue) + "%"
        #         sDeviceText = sDeviceText + "\n S2: " + str(modules[i].SoilSensor2.SensorValue) + "%"
        #         sDeviceText = sDeviceText + "\n S3: " + str(modules[i].SoilSensor3.SensorValue) + "%"

        #         Gui.lables[i].config(text=sDeviceText)

        for Device in modules.values():
            if not Device.bConnected:
                Device.connect()
            print("Device: {} Connected: {}".format(Device.ip, Device.bConnected))
            print(
                "Device: {} Humidity Value: {}%".format(
                    Device.ip, Device.sensors[0].curr_val
                )
            )
            # Device.SetRelay(not Device.RelayOut)
            # Device.SetDuration(Device.duration + 1)


# prgMain Thread
prg_Main = threading.Thread(target=prgMain)

# RUN PROGRAM
if __name__ == "__main__":
    # RUN prgMain
    prg_Main.start()
    # RUN GUI
    Gui.run()
    # gui.mainloop()
