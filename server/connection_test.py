import datetime
import logging
import random
from typing import Any, List, Dict
from local_dao import LocalDAO
from model import AnalogSensor, EPModule, SensorType, Logger
from repository import Repository, Command, Actions
from connections.mqtt import MQTTConnection
from django.utils.crypto import get_random_string


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
    db: LocalDAO = LocalDAO()
    first_module = createModules()
    id = db.add_module(first_module)
    if id != None:
        print("Succesfully added module! ID:", first_module.id)
    else:
        print("Something went wrong while trying to add a module!")


def add_module_wSensors():
    db: LocalDAO = LocalDAO()
    db.purge()
    db._build_tables()
    module = createModules(1, 2)

    id = db.add_module(module)
    if id != None:
        print("Succesfully added module! ID:", module.id)
    else:
        print("Something went wrong while trying to add a module!")


def createModules(mdul_cnt=1, snsor_cnt=0):
    def randint():
        return random.randint(1, 255)

    descriptions = [
        "Cactus",
        "Lilach",
        "Vegetables",
        "Lillys",
        "Fruits",
        "Pineapple",
        "Avocado",
        "Spices",
        "Herbs",
        "Cannabis",
    ]
    sensor_types = list(SensorType.__members__.values())

    modules = [
        EPModule(f"{randint()}.{randint()}.{randint()}.{randint()}")
        for _ in range(mdul_cnt)
    ]
    for module in modules:
        module.description = random.choice(descriptions)
        if snsor_cnt > 0:
            module.add_sensors(
                [
                    AnalogSensor(random.choice(sensor_types), get_random_string(8))
                    for _ in range(snsor_cnt)
                ]
            )
    return modules if len(modules) > 1 else modules[0]


def add_many_modules_wSensors():
    db = LocalDAO()
    db.purge()
    db._build_tables()
    modules = createModules(4, 3)
    result = db.add_modules(modules)
    if result:
        print(
            "Succesfully added",
            len(modules),
            "modules! ID's:",
            {module.id for module in modules},
        )
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


def get_modules():
    db = LocalDAO()
    modules = db.get_modules()
    for module in modules:
        print(module)


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
    module.add_sensors(
        [
            AnalogSensor(SensorType.EC, get_random_string(8)),
            AnalogSensor(SensorType.TEMPERATURE, get_random_string(8)),
            AnalogSensor(SensorType.EC, get_random_string(8)),
        ]
    )

    client = MQTTConnection("tester")
    client.connect("192.168.1.177", 1883)
    publish_dict: Dict[str, Any | List | Dict] = {
        "module": module.to_dict(),
        "sensors": [],
    }
    for sensor in module.sensors:
        publish_dict["sensors"].append(sensor.to_dict())
    client.publish("connected_devices", publish_dict)


def delete_module():
    db = LocalDAO()
    db.delete_module(1)


logging.setLoggerClass(Logger)
logging.getLogger().setLevel(logging.INFO)
add_many_modules_wSensors()
