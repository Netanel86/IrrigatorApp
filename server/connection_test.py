import datetime
from typing import List
from ModelLib import AnalogSensor, EPModule, Sensors
from repository import Repository, Command, Actions


def command_callback(cmnd_list: List[Command], timestamp: datetime.datetime):
    print(
        "[{0}] Commands recieved".format(timestamp.astimezone().strftime("%Y-%m-%d %X"))
    )
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


def add_module_wSensors():
    module = EPModule("0.1.0.0")
    module.description = "PYS#0"
    module.sensors = [
        AnalogSensor(Sensors.EC),
        AnalogSensor(Sensors.TEMPERATURE),
        AnalogSensor(Sensors.FLOW),
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
        module.sensors = [
            AnalogSensor(Sensors.EC),
            AnalogSensor(Sensors.TEMPERATURE),
            AnalogSensor(Sensors.FLOW),
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
    module.port = 8080
    module.description = "Python#0"
    module.max_duration = 3600
    module.duration = 100
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


repo = Repository()
# add_module_wSensors()
# add_batch_modules_wSensors()
# repo.init_command_listener(command_callback)
modules = repo.get_modules()
# dic = mods["0.0.0.0"].to_dict(("hell",))
# mod = EPModule.from_dict({"hello": 1})
# print(dic)
# update_module()
# add_single_module()
# add_batch_modules()
# repo.reset_databases()
repo.disconnect()
