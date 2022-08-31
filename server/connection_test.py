import datetime
from typing import List
from ModelLib import EPModule
from repository import Repository, Command, Actions


def command_callback(cmnd_list, timestamp):
    print(
        "[{0}] Commands recieved".format(timestamp.astimezone().strftime("%Y-%m-%d %X"))
    )
    for command in cmnd_list:

        print(command)
        # TODO execute command in each Action case
        match command.action:
            case Actions.OPEN:
                module = modules[command.attributes[Command.ATTR_INDEX]]
                module.on_time = datetime.datetime.now().astimezone()
                module.Duration = command.attributes[Command.ATTR_DURATION]
            case Actions.CLOSE:
                module = modules[command.attributes[Command.ATTR_INDEX]]
                module.on_time = datetime.datetime.now().astimezone()
                module.Duration = 0

        repository.update_module(
            module, [EPModule.PROP_DURATION, EPModule.PROP_ON_TIME]
        )

        # remove command after execution
        repository.delete_command(command)

def add_single_module():
    first_module = EPModule(0,0)
    first_module.description = 'PY#1'
    repository.add_module(first_module)

def add_batch_modules():
    modules = [EPModule(1,0),EPModule(2,0),EPModule(3,0),EPModule(4,0)]
    for module in modules:
        module.description = 'PY#{0}'.format(module.index + 1)
    repository.add_modules(modules)

def update_module_open():
    modules[0].on_time = datetime.datetime.now().astimezone()
    modules[0].Duration = 300
    repository.update_module(
            modules[0], [EPModule.PROP_DURATION, EPModule.PROP_ON_TIME]
        )
    
repository = Repository('lQxqLcM60RB33g9qOPeg')
# repository.init_command_listener(command_callback)

modules = repository.get_modules()
update_module_open()

input('wait for input\n')
repository.disconnect()
