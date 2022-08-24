import datetime
from typing import List
from ModelLib import EPModule
from repository import Repository, Command, Actions

def command_callback(cmnd_list, timestamp) :
        
        print('[{0}] Commands recieved'.format(timestamp.astimezone().strftime('%Y-%m-%d %X')))

        for command in cmnd_list:
            #execute command
            print(command) 

            match command.action:
                case Actions.OPEN:
                    module = modules[command.attributes[Command.ATTR_INDEX]]
                    module.on_time = datetime.datetime.now().astimezone()
                    module.Duration = command.attributes[Command.ATTR_DURATION]
                case Actions.CLOSE:
                    module = modules[command.attributes[Command.ATTR_INDEX]]
                    module.on_time = datetime.datetime.now().astimezone()
                    module.Duration = 0


            repository.update_module(module, [EPModule.PROP_DURATION, EPModule.PROP_ON_TIME])
            
            #remove command after execution
            repository.delete_command(command) 
        
repository = Repository()
repository.init_command_listener(command_callback)

# valve = Valve(4,1200)
# valve.description = "python"
# valve.id = connection.create_document('valves',valve.to_dict())
# print(valve)
# valve_snapshot = connection.read_document('valves','JCg10DTQ2iT1GCA8xoMU')
# valve = EPModule.from_dict(valve_snapshot.id, valve_snapshot.to_dict())


modules = repository.get_modules()
input('wait for input\n')
repository.disconnect()