from datetime import datetime
from enum import Enum
import os
import threading
from typing import Any, Callable, Dict, List
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from google.cloud.firestore import Client
from google.cloud.firestore_v1 import DocumentSnapshot, Query
from google.cloud.firestore_v1.watch import DocumentChange

import http.client as httplib

class Connection(object):
    KEY_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'irrigator-app-key.json')

    def __init__(self): 
        cred = credentials.Certificate(self.KEY_PATH)
        firebase_admin.initialize_app(cred)
        self.db: Client = firestore.client()
        self.notify_callback = threading.Event()
        self.listeners = {}

    def register_listener(self, col_id: str, callback: Callable[[List[DocumentSnapshot], List[DocumentChange], datetime], None]) -> None:
        """Registers a listener to collection changes.\n
        Args:
            col_id -- the collection id\n
            callback -- a function to execute when a callback is recieved\n
        Raises:
            ValueError: If the ``collection_id`` was already registered
        """
        def inner_callback(col_snapshot: List[DocumentSnapshot], changes: List[DocumentChange], read_time: datetime) -> None:
            callback(col_snapshot, changes, read_time)
            self.notify_callback.set()

        if col_id not in self.listeners:
                query = self.db.collection(col_id)
                listener = query.on_snapshot(inner_callback)
                self.listeners[col_id] = listener
        else:
            raise ValueError("Listener for '{0}' has already initialized".format(col_id))
        
    def unregister_listener(self, listener_id: str):
        """Removes a listener from the database.\n
        Args:
            listener_id -- the listener id
        """
        if listener_id in self.listeners.keys() :
            self.listeners[listener_id].unsubscribe()
            self.listeners.pop(listener_id)
    
    def create_document(self, col_id: str, doc_fields: Dict[str, Any]) -> str: 
        """Adds a document to the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_dict -- a dictionary with document properties, name and value pairs\n
        Returns: 
            the new document id.
        """
        new_ref = self.db.collection(col_id).document()
        doc_fields["id"] = new_ref.id
        new_ref.set(doc_fields)
        return new_ref.id
    
    def read_document(self, col_id: str, doc_id: str) -> DocumentSnapshot:
        """Fetch a document from a collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id\n
        Returns: 
            :class:`google.cloud.firestore_v1.DocumentSnapshot`: a snapshot of the requested document
        """
        doc_ref = self.db.collection(col_id).document(doc_id)
        doc = doc_ref.get()
        if not doc.exists:
            pass #raise exception

        return doc
    
    def read_collection(self, col_id: str) -> Query.stream:
        """Fetch an entire collection.\n
        Args:
            col_id -- the collection id\n
        Returns: 
            :class:`google.cloud.firestore_v1.Query.stream`: a query set of the requested document
        """
        return self.db.collection(col_id).stream()

    def update_document(self, col_id: str, doc_id: str, doc_fields: Dict[str, Any]):
        """Updates a document in the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id\n
            fields_dict -- dictionary with document properties, name and value pairs
        """
        doc_ref = self.db.collection(col_id).document(doc_id)
        doc_ref.update(doc_fields)

    def delete_document(self, col_id: str, doc_id: str):
        """Delete a document from the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id
        """
        self.db.collection(col_id).document(doc_id).delete()

    def check_connection(self):
        if not self.is_online():
            raise ConnectionError("No internet connection")
    
    def is_online(self) -> bool:
        """Returns TRUE if internet is available"""
        conn = httplib.HTTPSConnection("8.8.8.8", timeout=5)
        online = False
        try:
            conn.request("HEAD", "/")
            online = True
        except OSError as e:
            print(e)
            online = False
        finally:
            conn.close()
            return online

class Command(object):
    class Actions(Enum):
        REFRESH = 0
        OPEN = 1
        CLOSE = 2
        UPDATE = 3

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
        return {self.PROP_ACTION: self.action, 
                self.PROP_TIME: self.timestamp,
                self.PROP_ATTR: self.attributes}

    @staticmethod
    def from_dict(cmnd_id: str, source: Dict[str, Any]):
        """Parses and creates a new :class:`Command` from a dictionary.\n
        Args:
            cmnd_id -- the command's database id\n
            source -- a dictionary with object's properties, name and value pairs\n
        Returns:
            :class:`Command` -- a Command initialized with dictionary data\n
        Remarks:
            use the :class:`Command` constant property name fields: 
                :field:`Command.PROP_INDEX`\n
                :field:`Command.PROP_MSG`\n
                :field:`Command.PROP_LOG`\n
                :field:`Command.PROP_DURATION`\n
                :field:`Command.PROP_TIME`
        """
        return Command(cmnd_id,
                    source[Command.PROP_TIME], 
                    source[Command.PROP_ACTION], 
                    source[Command.PROP_ATTR])

    def __str__(self) -> str:
        to_string = str.format("[{0}: Command]:", self.timestamp.strftime('%Y-%m-%d %X'))

        match self.action:
            case 'on': to_string = "{0} Turn ON Valve #{1} for: {2}s".format(
                to_string, self.index, self.duration)
            case 'off': to_string = "{0} Turn OFF Valve #{1}".format(
                to_string, self.index)
            case 'description': to_string = "{0} Edit Valve #{1} description to: {2}".format(
                to_string, self.index, self.message)

        return to_string




