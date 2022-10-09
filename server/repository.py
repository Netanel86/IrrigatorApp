from __future__ import annotations
from collections import namedtuple
from typing import Any, Callable, Dict, List
from datetime import datetime
from enum import Enum
from ModelLib import DictParseable, EPModule
from firestore import FirestoreConnection, OrderBy, Where
from constants import Local, Remote
from sqlite import (
    COL_ROWID,
    QueryBuilder,
    SQLiteConnection,
)


def __reverse_dict(dict: Dict):
    return {val: key for val, key in dict}


MODULE_TO_LOCAL_MAP = {
    EPModule.Props().ID: Local.Modules.ColName.ID,
    EPModule.Props().IP: Local.Modules.ColName.IP,
    EPModule.Props().DESCRIPTION: Local.Modules.ColName.DESCRIPTION,
    EPModule.Props().MAX_DURATION: Local.Modules.ColName.MAX_DURATION,
    EPModule.Props().DURATION: Local.Modules.ColName.DURATION,
    EPModule.Props().ON_TIME: Local.Modules.ColName.ON_TIME,
    EPModule.Props().PORT: Local.Modules.ColName.PORT,
    EPModule.Props().TIMEOUT: Local.Modules.ColName.TIMEOUT,
}
"""A Mapping of `ModelLib.EPModule` properties to local database 'Module' table columns (key=prop_name, val=col_name)."""

MODULE_FROM_LOCAL_MAP = __reverse_dict(MODULE_TO_LOCAL_MAP.items())
"""A Mapping of local database 'Module' table columns to `ModelLib.EPModule` properties (key=col_name, val=prop_name)."""

MODULE_TO_REMOTE_MAP = {
    EPModule.Props().ID: Remote.Modules.FieldName.ID,
    EPModule.Props().IP: Remote.Modules.FieldName.IP,
    EPModule.Props().DESCRIPTION: Remote.Modules.FieldName.DESCRIPTION,
    EPModule.Props().MAX_DURATION: Remote.Modules.FieldName.MAX_DURATION,
    EPModule.Props().DURATION: Remote.Modules.FieldName.DURATION,
    EPModule.Props().ON_TIME: Remote.Modules.FieldName.ON_TIME,
}
"""A Mapping of `ModelLib.EPModule` properties to remote database 'Modules' collection fields (key=prop_name, val=field_name)."""

MODULE_FROM_REMOTE_MAP = __reverse_dict(MODULE_TO_REMOTE_MAP.items())
"""A Mapping of remote database 'Modules' collection fields to `ModelLib.EPModule` properties (key=field_name, val=prop_name)."""


class Repository(object):
    def __init__(self):
        self.__modules: Dict[str, EPModule] = {}
        self.__init_local()
        self.__init_remote()

    def __init_local(self):
        self.__local = SQLiteConnection()
        self.__local.create(Local.System.TABLE_NAME, Local.System.TYPE_MAP)
        self.__local.create(Local.Modules.TABLE_NAME, Local.Modules.TYPE_MAP)

    def __init_remote(self):
        self.__remote = FirestoreConnection()
        self.SYS_ID = self.__get_system_id()
        self.__init_paths(self.SYS_ID)

    def __init_paths(self, system_id: str):
        if system_id is None:
            raise ValueError(
                "system_id: value can't be None, call get_system_id() before initializing paths"
            )

        self.PATH_SYSTEM = "{0}/{1}".format(Remote.Systems.COLL_NAME, system_id)
        self.PATH_COMMANDS = "{0}/{1}".format(
            self.PATH_SYSTEM, Remote.Commands.COLL_NAME
        )
        self.PATH_MODULES = "{0}/{1}".format(self.PATH_SYSTEM, Remote.Modules.COLL_NAME)
        self.PATH_SENSORS = {}
        if self.__local is not None:
            module_ids = self.__local.select(
                Local.Modules.TABLE_NAME, (Local.Modules.ColName.ID,)
            ).execute()
            for id_dict in module_ids:
                self.PATH_SENSORS[
                    id_dict[Local.Modules.ColName.ID]
                ] = "{0}/{1}/{2}".format(
                    self.PATH_MODULES,
                    id_dict[Local.Modules.ColName.ID],
                    Remote.Sensors.COLL_NAME,
                )

    def __get_system_id(self) -> str:
        """Retrievs the system id.

        retrieve the system id from the local db, if one doesn't exist,
        request a new system reference id from the remote db.

        Returns:
            A new or existing system id.
        """
        sys_ans = (
            self.__local.select(Local.System.TABLE_NAME, (Local.System.ColName.ID,))
            .where((COL_ROWID,), (1,))
            .execute()
        )

        if sys_ans == False or len(sys_ans) == 0:
            system_id = self.__remote.document_ref(Remote.Systems.COLL_NAME)
            self.__local.insert(
                Local.System.TABLE_NAME, {Local.System.ColName.ID: system_id}
            )
        else:
            system_id = sys_ans[0][Local.System.ColName.ID]

        return system_id

    def add_module(self, module: EPModule):
        module.id = self.__remote.add_document(
            self.PATH_MODULES, module.to_dict(to_map=MODULE_TO_REMOTE_MAP)
        )

        ret_id = self.__local.insert(
            Local.Modules.TABLE_NAME,
            module.to_dict(to_map=MODULE_TO_LOCAL_MAP),
        )

        self.__modules[module.ip] = module

        return (
            module.id
            if len(module.id) > 0 and isinstance(ret_id, str) and module.id == ret_id
            else None
        )

    def add_modules(self, modules: List[EPModule]) -> int | None:
        remote_dicts = []
        for module in modules:
            remote_dicts.append(module.to_dict(to_map=MODULE_TO_REMOTE_MAP))

        doc_ids = self.__remote.add_documents(self.PATH_MODULES, remote_dicts)

        local_dicts = []
        for idx, module in enumerate(modules):
            module.id = doc_ids[idx]
            local_dicts.append(module.to_dict(to_map=MODULE_TO_LOCAL_MAP))
            self.__modules[module.ip] = module

        insert_count = self.__local.insert(Local.Modules.TABLE_NAME, local_dicts)
        return doc_ids if len(doc_ids) == insert_count else None

    def get_modules(self) -> Dict[str, EPModule]:
        if len(self.__modules) == 0:
            module_dicts = (
                self.__local.select(Local.Modules.TABLE_NAME)
                .orderby((Local.Modules.ColName.IP,))
                .execute()
            )

            self.__modules = self.__local.map_to_object(
                module_dicts,
                lambda dic: EPModule.from_dict(dic, MODULE_FROM_LOCAL_MAP),
                EPModule.Props().IP,
            )

        return self.__modules

    def get_commands(self) -> List[Command]:
        cmnd_dicts = self.__remote.get_collection(
            self.PATH_COMMANDS, OrderBy(Command.PROP_TIME, OrderBy.DESCENDING)
        )
        return self.__remote.map_to_object(
            cmnd_dicts,
            Remote.Commands.FieldName.ID,
            lambda dic: Command.from_dict(dic, COMMAND_FROM_REMOTE_MAP),
        )

    def update_module(
        self,
        module: EPModule,
        props: List[str] = None,
        local: bool = True,
        remote: bool = False,
    ):
        if not local and not remote:
            raise ValueError(
                "in {}.{}(): both 'local' and 'remote' values are set to False, nothing will be updated".format(
                    self.__class__.__name__, self.update_module.__name__
                )
            )
        if local:
            if props is not None:
                local_query: QueryBuilder = self.__local.update(
                    Local.Modules.TABLE_NAME,
                    module.to_dict(props, MODULE_TO_LOCAL_MAP),
                )
            else:
                local_query = self.__local.update(
                    Local.Modules.TABLE_NAME,
                    module.to_dict(to_map=MODULE_TO_LOCAL_MAP),
                )
            local_done = local_query.where(
                (Local.Modules.ColName.ID,), (module.id,)
            ).execute()

        if remote:
            if props is not None:
                rem_props = (prop for prop in props if prop in Remote.Modules.FIELDS)
                remot_dict = module.to_dict(rem_props, MODULE_TO_REMOTE_MAP)
            else:
                remot_dict = module.to_dict(to_map=MODULE_TO_REMOTE_MAP)

            self.__remote.update_document(self.PATH_MODULES, module.id, remot_dict)

        return local_done

    def init_command_listener(
        self, callback: Callable[[List[Command], datetime], None]
    ):
        self.__remote.register_listener(
            self.PATH_COMMANDS,
            lambda doc_dicts, timestamp: callback(
                self.__remote.map_to_object(
                    doc_dicts,
                    Remote.Commands.FieldName.ID,
                    lambda dic: Command.from_dict(dic, COMMAND_FROM_REMOTE_MAP),
                ),
                timestamp,
            ),
        )

    def delete_command(self, command: Command):
        self.__remote.delete_document(self.PATH_COMMANDS, command.id)

    def delete_module(self, module: EPModule):
        self.__remote.delete_document(self.PATH_MODULES, module.id)
        self.__modules.pop(module.ip)

    def reset_databases(self):
        ret = self.__remote.delete_collection(self.PATH_MODULES)
        print("Collection {} Deleted: {}".format(Remote.Modules.COLL_NAME, ret))

        ret = self.__remote.delete_document(
            Remote.Systems.COLL_NAME, self.__get_system_id()
        )
        print("Collection {} Deleted: {}".format(Remote.Systems.COLL_NAME, ret))

        ret = self.__local.delete(Local.Modules.TABLE_NAME)
        print("Table {} Deleted: {}".format(Local.Modules.TABLE_NAME, ret))

        ret = self.__local.delete(Local.System.TABLE_NAME)
        print("Table {} Deleted: {}".format(Local.System.TABLE_NAME, ret))

    def disconnect(self):
        self.__remote.disconnect()


class Actions(Enum):
    REFRESH = 0
    OPEN = 1
    CLOSE = 2
    UPDATE = 3


class Command(DictParseable):
    __Attributes = namedtuple("__Attributes", "IP DURATION DESCRIPTION")
    Attrs = __Attributes("ip", "duration", "description")

    __Properties = namedtuple("__Properties", "ID ACTION TIME ATTR")
    __Props = __Properties("id", "action", "timestamp", "attributes")

    @staticmethod
    def Props() -> __Properties:
        return Command.__Props

    def __init__(
        self, id: str, time: datetime, action: Actions, **attr: Dict[str, Any]
    ) -> None:
        self.id = id
        self.timestamp = time.astimezone()
        self.action = action
        self.attributes = attr

    def __str__(self) -> str:
        to_string = "[{0}: Command]:".format(self.timestamp.strftime("%Y-%m-%d %X"))

        match self.action:
            case Actions.OPEN:
                to_string = "{0} Turn ON Valve #{1} for: {2}s".format(
                    to_string,
                    self.attributes[Command.Attrs.IP],
                    self.attributes[Command.Attrs.DURATION],
                )
            case Actions.CLOSE:
                to_string = "{0} Turn OFF Valve #{1}".format(
                    to_string, self.attributes[Command.Attrs.IP]
                )
            case Actions.UPDATE:
                to_string = "{0} Edit Valve #{1} description to: {2}".format(
                    to_string,
                    self.attributes[Command.Attrs.IP],
                    self.attributes[Command.Attrs.DESCRIPTION],
                )

        return to_string


COMMAND_FROM_REMOTE_MAP = {
    Remote.Commands.FieldName.ID: Command.Props().ID,
    Remote.Commands.FieldName.ACTION: Command.Props().ACTION,
    Remote.Commands.FieldName.TIME: Command.Props().TIME,
    Remote.Commands.FieldName.ATTR: Command.Props().ATTR,
}
"""A Mapping of remote database 'Commands' collection fields to `repository.Command` properties (key=field_name, val=prop_name)."""
