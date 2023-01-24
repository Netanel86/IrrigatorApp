import datetime
import logging
import os, subprocess
from typing import Any, List, Dict
from unittest import result
from model import AnalogSensor, EPModule, SensorType, Logger
from repository import Repository, Command, Actions
from mqtt_manager import MQTTManager


def command_callback(cmnd_list: List[Command], timestamp: datetime.datetime):
    time = timestamp.astimezone().strftime("%Y-%m-%d %X")
    print(f"[{time}] Commands recieved")
    for command in cmnd_list:

        print(command)
        match command.action:
            case Actions.OPEN:
                module = modules[command.attributes[Command.Attrs.IP]]
                module.on_time = datetime.datetime.now().astimezone()
                module.duration = command.attributes[Command.Attrs.DURATION]
            case Actions.CLOSE:
                module = modules[command.attributes[Command.Attrs.IP]]
                module.on_time = datetime.datetime.now().astimezone()
                module.duration = 0

        repo.update_module(
            module, [EPModule.Props().DURATION, EPModule.Props().ON_TIME], remote=True
        )

        # remove command after execution
        repo.delete_command(command)


def add_single_module():
    first_module = EPModule("0.0.0.0")
    first_module.description = "PY#0"
    id = repo.add_module(first_module)
    if id != None:
        print("Succesfully added module! ID:", first_module.id)
    else:
        print("Something went wrong while trying to add a module!")


def add_two_modules_wSensor():
    module_1 = EPModule("192.168.0.201")
    module_1.description = "PY#0"
    module_1.add_sensors(AnalogSensor(SensorType.HUMIDITY))

    module_2 = EPModule("192.168.0.202")
    module_2.description = "PY#0"
    module_2.add_sensors(AnalogSensor(SensorType.HUMIDITY))

    id_1 = repo.add_module(module_1)
    id_2 = repo.add_module(module_2)
    if id_1 != None and id_2 != None:
        print("Succesfully added modules! IDs:{},{}".format(id_1, id_2))
    else:
        print("Something went wrong while trying to add a module!")


def add_module_wSensors():
    module = EPModule("0.1.0.0")
    module.description = "PYS#0"
    module.sensors = [
        AnalogSensor(SensorType.EC),
        AnalogSensor(SensorType.TEMPERATURE),
        AnalogSensor(SensorType.FLOW),
    ]
    id = repo.add_module(module)
    if id != None:
        print("Succesfully added module! ID:", module.id)
    else:
        print("Something went wrong while trying to add a module!")


def add_batch_modules_wSensors():
    modules = [
        EPModule("0.2.0.0"),
        EPModule("0.3.0.0"),
    ]
    for idx, module in enumerate(modules):
        module.description = "PYS#{}".format(idx + 1)
        module._sensors = [
            AnalogSensor(SensorType.EC),
            AnalogSensor(SensorType.TEMPERATURE),
            AnalogSensor(SensorType.FLOW),
        ]

    ids = repo.add_modules(modules)
    if ids != None:
        print("Succesfully added", len(ids), "modules! ID's:", ids)
    else:
        print("Something went wrong while trying to add a batch of modules!")


def add_batch_modules():
    modules = [
        EPModule("0.0.0.1"),
        EPModule("0.0.0.2"),
        EPModule("0.0.0.3"),
        EPModule("0.0.0.4"),
    ]
    for idx, module in enumerate(modules):
        module.description = "PY#{0}".format(idx + 1)

    ids = repo.add_modules(modules)
    if ids != None:
        print("Succesfully added", len(ids), "modules! ID's:", ids)
    else:
        print("Something went wrong while trying to add a batch of modules!")


def update_module():
    modules = repo.get_modules()
    module = modules["0.0.0.0"]
    module.__port = 8080
    module.description = "Python#0"
    module.__max_duration = 3600
    module.__duration = 100
    is_success = repo.update_module(
        module,
        [
            EPModule.Props().PORT,
            EPModule.Props().DESCRIPTION,
            EPModule.Props().MAX_DURATION,
            EPModule.Props().DURATION,
        ],
        remote=True,
    )
    if is_success:
        print("Succesfully updated module ID:", module.id)
    else:
        print("Something went wrong while trying to update module!")


def update_module_sensors():
    modules = repo.get_modules()
    module = modules["0.2.0.0"]
    res = False
    for sensor in module.__sensors:
        sensor.curr_val += 1
        if sensor.type == SensorType.TEMPERATURE.name:
            sensor.max_val = 200
        if sensor.type == SensorType.EC.name:
            sensor.max_val = 7

    res = repo.update_sensors(
        module,
        [
            AnalogSensor.Props().CURRENT_VAL,
            AnalogSensor.Props().MAX_VALUE,
        ],
        remote=True,
    )
    if res:
        print("Successfuly updated sensors.")
    else:
        print("Failed to update sensors.")


def test_dictParsable():
    module = EPModule("78:21:84:8C:AF:FC")
    module2 = EPModule("GG:21:22:8C:AF:11")
    module.id = "123"
    module2.id = "456"
    dict = module.to_dict()
    dict2 = module2.to_dict()
    module = EPModule.from_dict(dict)
    module2 = EPModule.from_dict(dict2)
    print(f"{module} + {module2}")


def test_mqtt():
    module = EPModule("88:21:84:8C:AF:FC")
    sens1 = AnalogSensor(SensorType.EC)
    sens1.id = "1"
    sens2 = AnalogSensor(SensorType.TEMPERATURE)
    sens2.id = "2"
    sens3 = AnalogSensor(SensorType.EC)
    sens3.id = "3"
    module.add_sensors(sens3)
    module.add_sensors([sens1, sens2])
    client = MQTTManager("tester")
    client.connect("192.168.1.177", 1883)
    publish_dict: Dict[str, Any | List | Dict] = {
        "module": module.to_dict(),
        "sensors": [],
    }
    for sensor in module.sensors:
        publish_dict["sensors"].append(sensor.to_dict())
    client.publish("connected_devices", publish_dict)


logging.setLoggerClass(Logger)
test_mqtt()
# logging.getLogger().setLevel(logging.INFO)
# test_dictParsable()


# repo = Repository()
# modules = repo.get_modules()
# module = modules["1:2:3:4:5"]
# module.id = "Hell YA!"
# module.description = "Tester"
# module.max_duration = 500
# module.duration = 25
# module.on_time = datetime.datetime.now().astimezone()
# module.add_sensors(AnalogSensor(SensorType.FLOW))
# module.sensors[0].curr_val = 20

# repo.disconnect()
