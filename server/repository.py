from __future__ import annotations
from mimetypes import init
from typing import Any, Callable, Dict, List
from datetime import datetime
from enum import Enum
from ModelLib import EPModule
from firestore import FirestoreConnection, OrderBy, Where


class Repository(object):
    __COL_COMMANDS = "commands"
    __COL_MODULES = "valves"
    __COL_SYSTEMS = "systems"

    def __init__(self, system_id: str) -> None:
        self.__client = FirestoreConnection()
        self.__modules: Dict[str, EPModule] = None

        if system_id is None:  # TODO remove after implementing local db
            # replace with self.__init_paths(self.__get_system_id())
            system_id = self.__get_system_id()
        self.__init_paths(system_id)

    def __init_paths(self, system_id: str):
        if system_id is None:
            raise ValueError(
                "system_id: value can't be None, call get_system_id() before initializing paths"
            )

        self.PATH_SYSTEM = "{0}/{1}".format(Repository.__COL_SYSTEMS, system_id)
        self.PATH_COMMANDS = "{0}/{1}".format(
            self.PATH_SYSTEM, Repository.__COL_COMMANDS
        )
        self.PATH_MODULES = "{0}/{1}".format(self.PATH_SYSTEM, Repository.__COL_MODULES)

    def __get_system_id(self) -> str:
        """Retrievs the system id.

        retrieves the system id from the local database, if one doesn't yet exist,
        will get a new system reference id from the client.

        Returns:
            A new or existing system id.
        """
        system_id = None  # TODO load system id from local db
        if system_id is None:
            system_id = self.__client.document_ref(Repository.__COL_SYSTEMS)

        return system_id

    def add_module(self, module: EPModule):
        module.id = self.__client.add_document(self.PATH_MODULES, module.to_dict())
        self.__modules[module.IP] = module

    def add_modules(self, modules: List[EPModule]):
        dicts = []
        for module in modules:
            dicts.append(module.to_dict())

        doc_ids = self.__client.add_documents(self.PATH_MODULES, dicts)

        for idx, module in enumerate(modules):
            module.id = doc_ids[idx]
            self.__modules[module.IP] = module

    def get_modules(self) -> Dict[str, EPModule]:
        if self.__modules is None:
            module_docs = self.__client.get_collection(
                self.PATH_MODULES, OrderBy(EPModule.PROP_IP)
            )
            self.__modules = self.__parse_modules(module_docs)

        return self.__modules

    def get_commands(self) -> List[Command]:
        return self.__client.get_collection(
            self.PATH_COMMANDS, OrderBy(Command.PROP_TIME, OrderBy.DESCENDING)
        )

    def update_module(self, module: EPModule, props: List[str] = None):
        update_dict = {}
        if props is not None:
            update_dict = module.to_prop_dict(props)
        else:
            update_dict = module.to_dict()

        self.__client.update_document(self.PATH_MODULES, module.id, update_dict)

    def init_command_listener(
        self, callback: Callable[[List[Command], datetime], None]
    ):
        def inner_callback(doc_dicts: Dict[str, Dict[str, Any]], timestamp):
            callback(self.__parse_commands(doc_dicts), timestamp)

        self.__client.register_listener(self.PATH_COMMANDS, inner_callback)

    def delete_command(self, command: Command):
        self.__client.delete_document(self.PATH_COMMANDS, command.id)

    def delete_module(self, module: EPModule):
        self.__client.delete_document(self.PATH_MODULES, module.id)
        self.__modules.pop(module.IP)

    def disconnect(self):
        self.__client.disconnect()

    def __parse_commands(self, cmnd_dicts: Dict[str, Dict[str, Any]]) -> List[Command]:
        commands: List[Command] = []
        for cmnd_id, cmnd_dict in cmnd_dicts.items():
            commands.append(Command.from_dict(cmnd_id, cmnd_dict))

        return commands

    def __parse_modules(
        self, module_dicts: Dict[str, Dict[str, Any]]
    ) -> Dict[str, EPModule]:
        modules: Dict[str, EPModule] = {}
        for module_id, module_dict in module_dicts.items():
            module = EPModule.from_dict(module_id, module_dict)
            modules[module.IP] = module

        return modules


class Actions(Enum):
    REFRESH = 0
    OPEN = 1
    CLOSE = 2
    UPDATE = 3


class Command(object):
    ATTR_IP = "index"
    ATTR_DURATION = "duration"
    ATTR_DESCRIPTION = "description"

    PROP_ID = "id"
    PROP_ACTION = "action"
    PROP_TIME = "timestamp"
    PROP_ATTR = "attributes"

    def __init__(
        self, id: str, time: datetime, action: Actions, attr: Dict[str, Any]
    ) -> None:
        self.id = id
        self.timestamp = time.astimezone()
        self.action = action
        self.attributes = attr

    def to_dict(self) -> Dict[str, Any]:
        """Parses :class:`Command` to a dictionary.\n
        Returns:
            A dictionary with this :class:`Command` propeties, name and value pairs"""
        return {
            self.PROP_ACTION: self.action.name,
            self.PROP_TIME: self.timestamp,
            self.PROP_ATTR: self.attributes,
        }

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
        return Command(
            cmnd_id,
            source[Command.PROP_TIME],
            Actions[source[Command.PROP_ACTION]],
            source[Command.PROP_ATTR],
        )

    def __str__(self) -> str:
        to_string = "[{0}: Command]:".format(self.timestamp.strftime("%Y-%m-%d %X"))

        match self.action:
            case Actions.OPEN:
                to_string = "{0} Turn ON Valve #{1} for: {2}s".format(
                    to_string,
                    self.attributes[Command.ATTR_IP],
                    self.attributes[Command.ATTR_DURATION],
                )
            case Actions.CLOSE:
                to_string = "{0} Turn OFF Valve #{1}".format(
                    to_string, self.attributes[Command.ATTR_IP]
                )
            case Actions.UPDATE:
                to_string = "{0} Edit Valve #{1} description to: {2}".format(
                    to_string,
                    self.attributes[Command.ATTR_IP],
                    self.attributes[Command.ATTR_DESCRIPTION],
                )

        return to_string
