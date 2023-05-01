import unittest
import datetime
import logging
import random
from typing import Any, List, Dict
from local_dao import XrefTypes, LocalDAO
from model import AnalogSensor, EPModule, SensorType, Logger
from repository import Repository, Command
from connections.mqtt import MQTTConnection
from django.utils.crypto import get_random_string

DESCRIPTIONS = [
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


def createModules(mdul_cnt=1, snsor_cnt=0):
    def randint():
        return random.randint(1, 255)

    sensor_types = list(SensorType.__members__.values())

    modules = [
        EPModule(f"{randint()}.{randint()}.{randint()}.{randint()}")
        for _ in range(mdul_cnt)
    ]
    for module in modules:
        module.description = random.choice(DESCRIPTIONS)
        if snsor_cnt > 0:
            module.add_sensors(
                [
                    AnalogSensor(random.choice(sensor_types), get_random_string(8))
                    for _ in range(snsor_cnt)
                ]
            )
    return modules if len(modules) > 1 else modules[0]


class TestLocalDao(unittest.TestCase):
    def __init__(self, methodName: str = "runTest") -> None:
        super().__init__(methodName)
        self.db: LocalDAO = LocalDAO()

    def test_add_module(self):
        first_module = createModules()
        id = self.db.add_module(first_module)

        self.assertIsNotNone(id, "value should not be None. Failed to add a module.")

    def test_get_modules(self):
        modules = list(self.db.get_modules().values())

        self.assertEqual(len(modules), 1, "value should equal 1. Failed to get module.")

    def test_add_module_with_sensors(self):
        module = createModules(1, 2)
        id = self.db.add_module(module)

        self.assertIsNotNone(id, "value should not be None. Failed to add a module.")
        self.assertIsNotNone(
            module.sensors[0].id, "value should not be None. Failed to add a sensor."
        )

    def test_add_multiple_modules_with_sensors(self):
        modules = createModules(4, 3)
        is_success = self.db.add_modules(modules)

        self.assertTrue(
            is_success, "value should not be False. Failed to add a module."
        )

    def test_update_module(self):
        modules = self.db.get_modules()
        module: EPModule = list(modules.values())[0]

        module.description = random.choice(DESCRIPTIONS)
        module.max_duration = 3600
        module.duration = 100
        is_success = self.db.update_module(
            module,
            [
                EPModule.Props().DESCRIPTION,
                EPModule.Props().MAX_DURATION,
                EPModule.Props().DURATION,
            ],
        )

        self.assertTrue(
            is_success, "value should not be False. Failed to update module."
        )

    def test_update_module_sensors(self):
        modules = self.db.get_modules()
        module = list(modules.values())[0]
        is_success = False
        for sensor in module.sensors:
            if sensor.type == SensorType.TEMPERATURE:
                sensor.max_val = 200
            if sensor.type == SensorType.EC:
                sensor.max_val = 7
            sensor.curr_val = random.randint(sensor.min_val, sensor.max_val)

        is_success = self.db.update_sensors(
            module.sensors,
            [
                AnalogSensor.Props().CURRENT_VAL,
                AnalogSensor.Props().MAX_VALUE,
            ],
        )

        self.assertTrue(
            is_success, "value should not be False. Failed to update sensors."
        )

    def test_add_remote_id(self):
        remote_id = get_random_string(12)
        result = self.db.add_remote_id(XrefTypes.Module, 1, remote_id)

        self.assertIsNotNone(
            result, "value should not be None. failed to add remote id"
        )

    def test_get_remote_id(self):
        remote_id = self.db.get_remote_id(XrefTypes.Module, 1)

        self.assertIsNotNone(
            remote_id, "value should not be None. failed to get remote id"
        )

    def test_delete_remote_id(self):
        result = self.db.delete_remote_id(XrefTypes.Module, 1)

        self.assertTrue(
            result, "value should not be False. Failed to delete remote id."
        )

    def test_purge(self):
        is_success = self.db.purge()

        self.assertTrue(
            is_success, "value should not be False. Failed to purge local database."
        )

    def suite() -> unittest.TestSuite:
        suite = unittest.TestSuite()
        suite.addTest(TestLocalDao("test_add_module"))
        suite.addTest(TestLocalDao("test_get_modules"))
        suite.addTest(TestLocalDao("test_add_module_with_sensors"))
        suite.addTest(TestLocalDao("test_add_multiple_modules_with_sensors"))
        suite.addTest(TestLocalDao("test_update_module"))
        suite.addTest(TestLocalDao("test_update_module_sensors"))
        suite.addTest(TestLocalDao("test_add_remote_id"))
        suite.addTest(TestLocalDao("test_get_remote_id"))
        suite.addTest(TestLocalDao("test_delete_remote_id"))
        suite.addTest(TestLocalDao("test_purge"))
        return suite


def command_callback(cmnd_list: List[Command], timestamp: datetime.datetime):
    time = timestamp.astimezone().strftime("%Y-%m-%d %X")
    print(f"[{time}] Commands recieved")
    for command in cmnd_list:

        print(command)
        match command.action:
            case Command.Actions.OPEN:
                module = modules[command.attributes[Command.Attrs.IP]]
                module.on_time = datetime.datetime.now().astimezone()
                module.duration = command.attributes[Command.Attrs.DURATION]
            case Command.Actions.CLOSE:
                module = modules[command.attributes[Command.Attrs.IP]]
                module.on_time = datetime.datetime.now().astimezone()
                module.duration = 0

        repo.update_module(
            module, [EPModule.Props().DURATION, EPModule.Props().ON_TIME], remote=True
        )

        # remove command after execution
        repo.delete_command(command)


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


logging.setLoggerClass(Logger)
logging.getLogger().setLevel(logging.INFO)

if __name__ == "__main__":
    runner = unittest.TextTestRunner(failfast=True)
    runner.run(TestLocalDao.suite())
