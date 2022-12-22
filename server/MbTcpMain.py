from tkinter import *  # export DISPLAY=:0.0  if runing from VS code SSH
import threading  # thread module imported
import os
import ModelLib
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


# -------GUI OBJECTS-------------


def close_window():
    global running
    running = False
    gui.destroy()
    print("Program closed")


gui = Tk(className="Python Examples - Window Size")
gui.protocol("WM_DELETE_WINDOW", close_window)
gui.configure(background="black")
# set window size
gui.geometry("1280x720")

# Lables
MainLable = Label(gui)
MainLable.config(height=1, width=30, bg="dimgray", fg="white", font=("ariel", 25))
# TempratureLable.pack(side=LEFT)
MainLable.place(x=0, y=0)

TimeLable = Label(gui)
TimeLable.config(height=2, bg="dimgray", fg="white", font=("ariel", 12))
TimeLable.place(x=540, y=0)

TempratureLable = Label(gui)
TempratureLable.config(bg="black", fg="white", font=("ariel", 16))
TempratureLable.place(x=50, y=310)

SoilSensor_1_Lable = Label(gui)
SoilSensor_1_Lable.config(bg="black", fg="white", font=("ariel", 16))
SoilSensor_1_Lable.place(x=50, y=250)

SoilSensor_2_Lable = Label(gui)
SoilSensor_2_Lable.config(bg="black", fg="white", font=("ariel", 16))
SoilSensor_2_Lable.place(x=50, y=280)
# buttons


def V1ButtonOnClick():
    Devices[0].SetRelay(not Devices[0].RelayOut)


def V2ButtonOnClick():
    Devices[1].SetRelay(not Devices[1].RelayOut)


def V3ButtonOnClick():
    Devices[2].SetRelay(not Devices[2].RelayOut)


def V4ButtonOnClick():
    Devices[3].SetRelay(not Devices[3].RelayOut)


gButtons = [
    Button(
        gui,
        text="V1 OFF",
        bg="red",
        width=10,
        height=2,
        font=("Helvetica", 20),
        activebackground="red",
        command=V1ButtonOnClick,
    ),
    Button(
        gui,
        text="V1 OFF",
        bg="red",
        width=10,
        height=2,
        font=("Helvetica", 20),
        activebackground="red",
        command=V2ButtonOnClick,
    ),
    Button(
        gui,
        text="V1 OFF",
        bg="red",
        width=10,
        height=2,
        font=("Helvetica", 20),
        activebackground="red",
        command=V3ButtonOnClick,
    ),
    Button(
        gui,
        text="V1 OFF",
        bg="red",
        width=10,
        height=2,
        font=("Helvetica", 20),
        activebackground="red",
        command=V4ButtonOnClick,
    ),
]
gButtons[0].place(x=20, y=60)
gButtons[1].place(x=220, y=60)
gButtons[2].place(x=20, y=150)
gButtons[3].place(x=220, y=150)

gDevicesSensors = [
    Label(gui, height=6, text="Device 1", bg="black", fg="white", font=("ariel", 18)),
    Label(gui, height=6, text="Device 2", bg="black", fg="white", font=("ariel", 18)),
    Label(gui, height=6, text="Device 3", bg="black", fg="white", font=("ariel", 18)),
]
gDevicesSensors[0].place(x=20, y=250)
gDevicesSensors[1].place(x=340, y=250)
gDevicesSensors[2].place(x=660, y=250)


# -------END GUI OBJECTS-------------


def prgMain():
    while running:
        time.sleep(0.4)

        for i in range(len(gButtons)):

            if Devices[i].bConnected:
                Devices[i].ReadClentRegs()
                if i <= 2:
                    print("Device: " + str(Devices[i].ip) + str(Devices[i].bConnected))
                # print("Device " + str(Devices[i].IP))
                if Devices[i].RelayOut:
                    buttonText = "ON -> " + str(Devices[i].REMAINING_TIME)
                    gButtons[i].config(
                        text=buttonText,
                        fg="black",
                        bg="green",
                        activebackground="green",
                    )
                else:
                    buttonText = "OFF"
                    gButtons[i].config(
                        text=buttonText, fg="white", bg="gray", activebackground="gray"
                    )
            else:
                # print("Device " + str(Devices[i].IP) + "  ComError!!!")
                buttonText = "Com Error"
                gButtons[i].config(
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
            # print("Device " + Device.IP + "  Soil Sensors Value(%): "+ str(Device.GetSensors()))
            # Device.SetRelay(not Device.RelayOut)
            # Device.SetDuration(Device.duration + 1)


# prgMain Thread
prg_Main = threading.Thread(target=prgMain)

# RUN PROGRAM
if __name__ == "__main__":
    # RUN prgMain
    prg_Main.start()
    # RUN GUI
    gui.mainloop()
