from tkinter import *
from typing import Callable, List, Dict

class GUI(object):
    def __init__(self) -> None:
        self.gui = Tk(className="Python Examples - Window Size")
        self.gui.protocol("WM_DELETE_WINDOW", self.close_window)
        self.gui.configure(background="black")

        # set window size
        self.gui.geometry("1280x720")

        self.__init_lables()
        self.next_sensor_x: int = 0

        self.lables: Dict[str,Label] = {}
        self.buttons: Dict[str,Button] = {}


    def __init_lables(self):
        # Lables
        MainLable = Label(self.gui)
        MainLable.config(
            height=1, width=30, bg="dimgray", fg="white", font=("ariel", 25)
        )
        MainLable.place(x=0, y=0)

        TimeLable = Label(self.gui)
        TimeLable.config(height=2, bg="dimgray", fg="white", font=("ariel", 12))
        TimeLable.place(x=540, y=0)

        TempratureLable = Label(self.gui)
        TempratureLable.config(bg="black", fg="white", font=("ariel", 16))
        TempratureLable.place(x=50, y=310)

        # SoilSensor_1_Lable = Label(self.gui)
        # SoilSensor_1_Lable.config(bg="black", fg="white", font=("ariel", 16))
        # SoilSensor_1_Lable.place(x=50, y=250)

        # SoilSensor_2_Lable = Label(self.gui)
        # SoilSensor_2_Lable.config(bg="black", fg="white", font=("ariel", 16))
        # SoilSensor_2_Lable.place(x=50, y=280)

    def add_button(self, key: str, on_click: Callable[[None], None]):
        row_size = 2
        button = Button(
            self.gui,
            text="V1 OFF",
            bg="red",
            width=10,
            height=2,
            font=("Helvetica", 20),
            activebackground="red",
            command=on_click,
        )
        pos_x = 20 if (len(self.buttons) % 2) == 0 else 220
        pos_y = 60 if len(self.buttons) < row_size else 150
        button.place(x=pos_x, y=pos_y)
        self.buttons[key] = button

    def add_label(self, key:str, text: str):
        sensor_label = Label(
                self.gui,
                height=6,
                text=text,
                bg="black",
                fg="white",
                font=("ariel", 18),
            )
        self.next_sensor_x = 20 if self.next_sensor_x == 0 else (self.next_sensor_x + 330) 
        sensor_label.place(x=self.next_sensor_x,y=250)
        self.lables[key] = sensor_label

    def run(self):
        self.gui.mainloop()

    def set_on_close_callback(self, on_close: Callable[[None], None]):
        self.on_close = on_close

    def close_window(self):
        if self.on_close is not None:
            self.on_close()
        self.gui.destroy()
        print("Program closed")
