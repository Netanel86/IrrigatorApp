from __future__ import annotations
from typing import Any, Callable, Dict, List
from datetime import datetime

import threading
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from google.cloud.firestore import Client
from google.cloud.firestore_v1 import DocumentSnapshot, Query
from google.cloud.firestore_v1.watch import DocumentChange, Watch
import http.client as httplib

class FireBaseConnection(object):
    
    def __init__(self, certificate_key): 
        cred = credentials.Certificate(certificate_key)
        firebase_admin.initialize_app(cred)
        
        self.__db: Client = firestore.client()
        self.__callback_thread = threading.Event()
        self.__listeners: Dict[str, Watch] = {}
    
    async def add_document(self, col_id: str, doc_fields: Dict[str, Any]) -> str: 
        """Adds a document to the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_dict -- a dictionary with document properties, name and value pairs\n
        Returns: 
            the new document id.
        """
        new_ref = self.__db.collection(col_id).document()
        doc_fields["id"] = new_ref.id
        await new_ref.set(doc_fields)
        return new_ref.id
    
    async def get_document(self, col_id: str, doc_id: str) -> DocumentSnapshot:
        """Gets a document from a collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id\n
        Returns: 
            :class:`firestore_v1.DocumentSnapshot`: a snapshot of the requested document
        """
        doc_ref = self.__db.collection(col_id).document(doc_id)
        doc = await doc_ref.get()
        if not doc.exists:
            pass #raise exception

        return doc
    
    def get_collection(self, col_id: str) -> QueryBuilder:
        """Gets a collection query builder.\n
        Args:
            col_id -- the collection id\n
        Returns: 
            :class:`QueryBuilder`: a collection query builder
        """
        return QueryBuilder(self.__db, col_id)

    async def update_document(self, col_id: str, doc_id: str, doc_fields: Dict[str, Any]):
        """Updates a document in the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id\n
            doc_fields -- dictionary with document properties, name and value pairs
        """
        doc_ref = self.__db.collection(col_id).document(doc_id)
        await doc_ref.update(doc_fields)

    async def delete_document(self, col_id: str, doc_id: str):
        """Delete a document from the collection.\n
        Args:
            col_id -- the collection id containing the document\n
            doc_id -- the document id
        """
        await self.__db.collection(col_id).document(doc_id).delete()
    
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
            self.__callback_thread.set()

        if col_id not in self.__listeners:
                query = self.__db.collection(col_id)
                listener = query.on_snapshot(inner_callback)
                self.__listeners[col_id] = listener
        else:
            raise ValueError("Listener for '{0}' has already initialized".format(col_id))
        
    def unregister_listener(self, listener_id: str) -> None:
        """Removes a listener from the database.\n
        Args:
            listener_id -- the listener id
        """
        if listener_id in self.__listeners.keys() :
            self.__listeners[listener_id].unsubscribe()
            self.__listeners.pop(listener_id)

    def disconnect(self) :
        for listener in self.__listeners.values():
            listener.unsubscribe()
        self.__listeners.clear()
        self.__callback_thread.clear()
        self.__db.close()
   
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

class QueryBuilder(object):

    def __init__(self, db: Client, col_id: str) -> None:
        self.__collection_ref = db.collection(col_id)
        self.__query: Query = None
    
    def order_by(self, field: str, direction: str = Query.ASCENDING) -> QueryBuilder:
        if self.__query is None:
            self.__query = self.__collection_ref.order_by(field, direction= direction)
        else:
            self.__query.order_by(field, direction= direction)
        return self
    
    def where(self, field: str, operator: str, value: Any) -> QueryBuilder:
        if self.__query is None:
            self.__query = self.__collection_ref.where(field, operator, value)
        else:
            self.__query.where(field, operator, value)
        return self

    def execute(self)-> List[DocumentSnapshot]: 
        if self.__query is None:
            return self.__collection_ref.stream()
        else:
            return self.__query.stream()






