import datetime
from typing import List
from connection import Connection, Command
from ModelLib import EPModule

def command_callback(col_snapshot, changes, read_time) :
    cmnd_list : List[Command] = []
    if len(col_snapshot) > 0:
        for doc in col_snapshot:
            cmnd_list.append(Command.from_dict(doc.id, doc.to_dict()))
        
        print('[{0}] Command recieved'.format(read_time.astimezone().strftime('%Y-%m-%d %X')))

        for command in cmnd_list:
            #execute command
            print(command) 

            match command.action:
                case Command.Actions.OPEN:
                    module = modules[command.attributes["index"]]
                    module.on_time = datetime.datetime.now().astimezone()
                    module.Duration = command.attributes["duration"]
                case Command.Actions.CLOSE:
                    module = modules[command.attributes["index"]]
                    module.on_time = datetime.datetime.now().astimezone()
                    module.Duration = 0


            connection.update_document(
                'valves', 
                module.id, 
                module.to_prop_dict([EPModule.PROP_DURATION, EPModule.PROP_ON_TIME]))
            
            #remove command after execution
            connection.delete_document('commands',command.id) 
        
connection = Connection()
connection.register_listener('commands', command_callback)

# valve = Valve(4,1200)
# valve.description = "python"
# valve.id = connection.create_document('valves',valve.to_dict())
# print(valve)
# valve_snapshot = connection.read_document('valves','JCg10DTQ2iT1GCA8xoMU')
# valve = EPModule.from_dict(valve_snapshot.id, valve_snapshot.to_dict())

module_docs = connection.read_collection('valves')
modules : List[EPModule] = []
for doc in module_docs:
    modules.append(EPModule.from_dict(doc.id, doc.to_dict()))


input('wait for input\n')
connection.unregister_listener('commands')