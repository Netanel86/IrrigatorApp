from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os
import ModelLib
from gui import GUI
from ModelLib import AnalogSensor, SensorType
import time


# check if execute from SSH and run GUI on the remote Device
if os.environ.get("DISPLAY", "") == "":
    print("no display found. Using :0.0")
    os.environ.__setitem__("DISPLAY", ":0.0")

# Global Parameters
running = True
bFirstCycle = True
GlobalCounter = 0
Devices = [
    ModelLib.EPModule(ip="192.168.0.201"),
    ModelLib.EPModule(ip="192.168.0.202"),
    ModelLib.EPModule(ip="192.168.0.213"),
    ModelLib.EPModule(ip="192.168.0.212"),
]

Devices[0].sensors = [
    AnalogSensor(SensorType.HUMIDITY)
]

def on_close():
    global running
    running = False

Gui = GUI()
Gui.set_on_close_event(on_close)

for module in Devices:
    Gui.add_button(lambda: module.SetRelay(not module.RelayOut))

def prgMain():
    while running:
        time.sleep(0.4)
        for i in range(len(Gui.buttons)):

            if Devices[i].bConnected:
                Devices[i].ReadClentRegs()
                if i <= 2:
                    print("Device: " + str(Devices[i].ip) + str(Devices[i].bConnected))
                # print("Device " + str(Devices[i].IP))
                if Devices[i].RelayOut:
                    buttonText = "ON -> " + str(Devices[i].REMAINING_TIME)
                    Gui.buttons[i].config(
                        text=buttonText,
                        fg="black",
                        bg="green",
                        activebackground="green",
                    )
                else:
                    buttonText = "OFF"
                    Gui.buttons[i].config(
                        text=buttonText, fg="white", bg="gray", activebackground="gray"
                    )
            else:
                # print("Device " + str(Devices[i].IP) + "  ComError!!!")
                buttonText = "Com Error"
                Gui.buttons[i].config(
                    text=buttonText, fg="black", bg="red", activebackground="red"
                )

        # for i in range(len(gDevicesSensors)):
        #         sDeviceText = "Device " + str(Devices[i].ip) + "\n S1: " + str(Devices[i].SoilSensor1.SensorValue) + "%"
        #         sDeviceText = sDeviceText + "\n S2: " + str(Devices[i].SoilSensor2.SensorValue) + "%"
        #         sDeviceText = sDeviceText + "\n S3: " + str(Devices[i].SoilSensor3.SensorValue) + "%"

        #         gDevicesSensors[i].config(text=sDeviceText)

        for Device in Devices:
            if not Device.bConnected:
                Device.connect()
            print("Device: " + str(Device.ip) + str(Device.bConnected))
            print("Device " + Device.ip + str(Device.bConnected))
            print("Device " + Device.ip + "  Soil Sensors Value(%): "+ str(Device.sensors))
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
