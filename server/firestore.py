from __future__ import annotations
from typing import Any, Callable, Dict, List, Literal, Tuple
from datetime import datetime
import os
import threading
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from google.cloud.firestore import Client
from google.cloud.firestore_v1 import (
    DocumentSnapshot,
    DocumentReference,
    CollectionReference,
    Query,
)
from google.cloud.firestore_v1.watch import Watch
import http.client as httplib


class FirestoreConnection(object):
    __KEY_PATH = os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "irrigator-app-key.json"
    )
    __COL_COMMANDS = "commands"
    __COL_MODULES = "valves"
    __COL_SYSTEMS = "systems"

    def __init__(self):
        self.__init_firestore()
        self.__callback_command = threading.Event()
        self.__listeners: Dict[str, Watch] = {}

    def __is_exist(self, col_id: str, doc_id: str) -> bool:
        self.__db.collection(col_id).document()

    def __init_firestore(self):
        cred = credentials.Certificate(FirestoreConnection.__KEY_PATH)
        firebase_admin.initialize_app(cred)
        self.__db: Client = firestore.client()

    def __init_paths(self, system_id: str):
        self.PATH_SYSTEM = "{0}/{1}".format(
            FirestoreConnection.__COL_SYSTEMS, system_id
        )
        self.PATH_COMMANDS = "{0}/{1}".format(
            self.PATH_SYSTEM, FirestoreConnection.__COL_COMMANDS
        )
        self.PATH_MODULES = "{0}/{1}".format(
            self.PATH_SYSTEM, FirestoreConnection.__COL_MODULES
        )

    def init_user_system(self, system_id: str, is_new: bool = False) -> str:
        if is_new:
            system_id = self.__document_ref(self.__COL_SYSTEMS).id
            self.__init_paths(system_id)
        else:
            if system_id is not None:
                # TODO check if exists:
                # self.__document_ref(self.__COL_SYSTEMS, system_id)
                self.__init_paths(system_id)
            else:
                raise ValueError(
                    "system_id: value must be set for an existing system, otherwise set property is_new = TRUE to create a new one"
                )
        return system_id

    def add_document(self, col_path: str, doc_fields: Dict[str, Any]) -> str:
        """Adds a document to the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_dict -- a dictionary with document properties, name and value pairs\n
        Returns:
            the new document id.
        """
        new_ref = self.__db.collection(col_path).document()
        new_ref.set(doc_fields)
        return new_ref.id

    def add_documents(self, col_path: str, docs: List[Dict[str, Any]]) -> List[str]:
        batch = self.__db.batch()
        doc_ids: List[str] = []

        for doc in docs:
            doc_ref = self.__db.collection(col_path).document()
            doc_ids.append(doc_ref.id)
            batch.set(doc_ref, doc)

        batch.commit()
        return doc_ids

    def get_document(self, col_path: str, doc_id: str) -> Tuple[str, Dict[str, Any]]:
        """Gets a document from a collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id\n
        Returns:
            Dict: a snapshot of the requested document
        """
        snapshot = self.__db.collection(col_path).document(doc_id).get()
        return (snapshot.id, snapshot.to_dict())

    def update_document(
        self, col_path: str, doc_id: str, updated_props: Dict[str, Any]
    ):
        """Updates a document in the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id\n
            doc_fields -- dictionary with document properties, name and value pairs
        """
        self.__db.collection(col_path).document(doc_id).update(updated_props)

    def get_collection(
        self, col_path: str, orderby: OrderBy = None, where: Where = None
    ) -> Dict[str, Dict[str, Any]]:
        query: Query | CollectionReference = self.__db.collection(col_path)

        if orderby is not None:
            query = query.order_by(orderby.property, direction=orderby.direction)

        if where is not None:
            query = query.where(where.property, where.operator, where.value)

        return self.__parse_collection(query.stream())

    def delete_document(self, col_path: str, doc_id: str):
        """Delete a document from the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id
        """
        self.__db.collection(col_path).document(doc_id).delete()

    def register_listener(
        self,
        col_path: str,
        callback: Callable[[Dict[str, Dict[str, Any]], datetime], None],
    ):
        """Registers a listener to collection changes.\n
        Args:
            col_id -- the collection id\n
            callback -- a function to execute when a callback is recieved\n
        Raises:
            ValueError: If the ``collection_id`` was already registered

        """

        def inner_callback(col_snapshots, changes, timestamp):
            if len(col_snapshots) > 0:
                coll_dicts = self.__parse_collection(col_snapshots)
                callback(coll_dicts, timestamp)
                self.__callback_command.set()

        if col_path not in self.__listeners:
            query = self.__db.collection(col_path)
            listener = query.on_snapshot(inner_callback)
            self.__listeners[col_path] = listener
        else:
            raise ValueError(
                "Listener for '{0}' has already initialized".format(col_path)
            )

    def unregister_listener(self, listener_id: str):
        """Removes a listener from the database.\n
        Args:
            listener_id -- the listener id
        """
        if listener_id in self.__listeners.keys():
            self.__listeners[listener_id].unsubscribe()
            self.__listeners.pop(listener_id)

    def disconnect(self):
        for listener in self.__listeners.values():
            listener.unsubscribe()
        self.__listeners.clear()
        self.__callback_command.clear()
        self.__db.close()

    def __document_ref(self, col_path: str, doc_id: str = None) -> DocumentReference:
        return self.__db.collection(col_path).document(doc_id)

    def __parse_collection(
        self, snapshot: List[DocumentSnapshot]
    ) -> Dict[str, Dict[str, Any]]:
        docs: Dict[str, Dict[str, Any]] = {}
        for doc in snapshot:
            docs[doc.id] = doc.to_dict()

        return docs

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


class OrderBy(object):
    ASCENDING = 0
    DESCENDING = 1

    def __init__(self, property: str, direction: Literal = ASCENDING) -> None:
        self.property = property

        if direction is OrderBy.ASCENDING:
            self.direction = Query.ASCENDING
        else:
            self.direction = Query.DESCENDING


class Where(object):
    def __init__(self, property: str, operator: str, value: Any) -> None:
        self.property = property
        self.operator = operator
        self.value = value
