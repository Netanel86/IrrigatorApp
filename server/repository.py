from __future__ import annotations
from typing import Any, Callable, Dict, List, Literal
from datetime import datetime
from enum import Enum
import os
from ModelLib import EPModule

from firebase_connection import FireBaseConnection

class Repository(object):
    __KEY_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'irrigator-app-key.json')
    PATH_COMMANDS = 'commands'
    PATH_MODULES = 'valves'

    def __init__(self) -> None:
        self.__connection = FireBaseConnection(Repository.__KEY_PATH)
        self.__modules: List[EPModule] = None
    
    def get_modules(self) -> List[EPModule]:
        if(self.__modules is None):
            self.__modules = []
            col_query = self.__connection.get_collection(Repository.PATH_MODULES).order_by(EPModule.PROP_INDEX)
            module_docs = col_query.execute()
            for doc in module_docs:
                self.__modules.append(EPModule.from_dict(doc.id, doc.to_dict()))
        return self.__modules

    def update_module(self, module: EPModule, props: List[str] = None) -> None:
        update_dict = {}
        if props is not None:
            update_dict = module.to_prop_dict(props)
        else:
            update_dict = module.to_dict()
        
        self.__connection.update_document(
                Repository.PATH_MODULES, module.id, update_dict)

    def add_module(self, module: EPModule):
        self.__connection.add_document(Repository.PATH_MODULES, module.to_dict())

    def delete_command(self, command: Command) -> None:
        self.__connection.delete_document(Repository.PATH_COMMANDS, command.id)

    def init_command_listener(self, callback: Callable[[List[Command], datetime], None]) -> None:
        def inner_callback(col_snapshot, changes, timestamp):
            cmnd_list : List[Command] = []
            if len(col_snapshot) > 0:
                for doc in col_snapshot:
                    cmnd_list.append(Command.from_dict(doc.id, doc.to_dict()))
                callback(cmnd_list, timestamp)

        self.__connection.register_listener(Repository.PATH_COMMANDS, inner_callback)
    
    def disconnect(self):
        self.__connection.disconnect();

class Actions(Enum):
    REFRESH = 0
    OPEN = 1
    CLOSE = 2
    UPDATE = 3

class Command(object):
    ATTR_INDEX = 'index'
    ATTR_DURATION = 'duration'
    ATTR_DESCRIPTION = 'description'

    PROP_ID = 'id'
    PROP_ACTION = 'action'
    PROP_TIME = 'timestamp'
    PROP_ATTR =  'attributes'

    def __init__(self, id: str, time: datetime, action: Actions, attr: Dict[str, Any]) -> None:
        self.id = id
        self.timestamp = time.astimezone()
        self.action = action
        self.attributes = attr
    
    def to_dict(self) -> Dict[str, Any]:
        """Parses :class:`Command` to a dictionary.\n
        Returns:
            A dictionary with this :class:`Command` propeties, name and value pairs"""
        return {self.PROP_ACTION: self.action.name, 
                self.PROP_TIME: self.timestamp,
                self.PROP_ATTR: self.attributes}

    @staticmethod
    def from_dict(cmnd_id: str, source: Dict[str, Any]):
        """Parses and creates a new :class:`Command` from a dictionary.\n
        Args:
            cmnd_id -- the command's database id\n
            source -- a dictionary with command class properties, name and value pairs\n
        Returns:
            :class:`Command` -- a Command initialized with dictionary data\n
        Remarks:
            use the :class:`Command` constant property name fields: 
                :field:`Command.PROP_ACTION`\n
                :field:`Command.PROP_TIME`\n
                :field:`Command.PROP_ATTR`
        """
        return Command(cmnd_id,
                    source[Command.PROP_TIME], 
                    Actions[source[Command.PROP_ACTION]], 
                    source[Command.PROP_ATTR])

    def __str__(self) -> str:
        to_string = "[{0}: Command]:".format(self.timestamp.strftime('%Y-%m-%d %X'))

        match self.action:
            case Actions.OPEN: 
                to_string = '{0} Turn ON Valve #{1} for: {2}s'.format(to_string, 
                    self.attributes[Command.ATTR_INDEX], 
                    self.attributes[Command.ATTR_DURATION])
            case Actions.CLOSE: 
                to_string = '{0} Turn OFF Valve #{1}'.format(to_string, 
                    self.attributes[Command.ATTR_INDEX])
            case Actions.UPDATE: 
                to_string = '{0} Edit Valve #{1} description to: {2}'.format(to_string, 
                    self.attributes[Command.ATTR_INDEX], 
                    self.attributes[Command.ATTR_DESCRIPTION])

        return to_string