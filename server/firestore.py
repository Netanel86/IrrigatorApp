from __future__ import annotations
from typing import Any, Callable, Dict, List, Literal, Tuple, Type
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

    def __init__(self):
        self.__init_firestore()
        self.__callback_command = threading.Event()
        self.__listeners: Dict[str, Watch] = {}

    def exist(self, col_id: str, doc_id: str) -> bool:
        return self.__db.collection(col_id).document(doc_id).get().exists

    def __init_firestore(self):
        cred = credentials.Certificate(FirestoreConnection.__KEY_PATH)
        firebase_admin.initialize_app(cred)
        self.__db: Client = firestore.client()

    def add_document(self, col_path: str, doc_fields: Dict[str, Any]) -> str:
        """Adds a new document to the collection.

        Args:
            col_path: The collection path containing the document.
            doc_fields: A dictionary with document properties, name and value pairs.

        Returns:
            The new document id.
        """
        new_ref = self.__db.collection(col_path).document()
        new_ref.set(doc_fields)
        return new_ref.id

    def add_documents(self, col_path: str, docs: List[Dict[str, Any]]) -> List[str]:
        """Adds multiple documents to the collection.

        Args:
            col_path: The collection path containing the document.
            doc_fields: A dictionary with document properties, name and value pairs.

        Returns:
            A list of new document id's, ordered in the original list order.
        """
        batch = self.__db.batch()
        doc_ids: List[str] = []

        for doc in docs:
            doc_ref = self.__db.collection(col_path).document()
            doc_ids.append(doc_ref.id)
            batch.set(doc_ref, doc)

        batch.commit()
        return doc_ids

    def get_document(self, col_path: str, doc_id: str) -> Tuple[str, Dict[str, Any]]:
        """Gets an existing document from the collection.

        Args:
            col_path: The collection path containing the document.
            doc_id: The document id.

        Returns:
            A tuple containing document id and document dictionary.
        """
        snapshot = self.__db.collection(col_path).document(doc_id).get()
        return (snapshot.id, snapshot.to_dict())

    def update_document(
        self, col_path: str, doc_id: str, updated_props: Dict[str, Any]
    ):
        """Updates an existing document in the collection.

        Args:
            col_path: The collection path containing the document.
            doc_id: The document id to update.
            updated_props: A dictionary with document properties, name and value pairs.
        """
        self.__db.collection(col_path).document(doc_id).update(updated_props)

    def get_collection(
        self, col_path: str, orderby: OrderBy = None, where: Where = None
    ) -> Dict[str, Dict[str, Any]]:
        """Retrieve an entire collection.

        Args:
            col_path: The collection id.
            orderby(optional): A :class:`firestore.OrderBy` class describing a direction and
                a field for ordering documents.
            where(optional): A :class:`firestore.Where` class describing a condition for
                filtering documents.

        Returns:
            A documents dictionary with document id as key.
        """
        query: Query | CollectionReference = self.__db.collection(col_path)

        if orderby is not None:
            query = query.order_by(orderby.field, direction=orderby.direction)

        if where is not None:
            query = query.where(where.field, where.operator, where.value)

        return self.__parse_collection(query.stream())

    def delete_document(self, col_path: str, doc_id: str):
        """Deletes a document from the collection.

        Args:
            col_path: The collection path containing the document.
            doc_id: The document id to delete.
        """
        self.__db.collection(col_path).document(doc_id).delete()

    def delete_collection(self, col_path: str) -> int:
        docs = self.__db.collection(col_path).stream()
        deleted = 0
        for doc in docs:
            ref: DocumentReference = doc.reference
            ref.delete()
            deleted += 1
        return deleted

    def register_listener(
        self,
        col_path: str,
        callback: Callable[[Dict[str, Dict[str, Any]], datetime], None],
    ):
        """Registers a callback for collection or document changes.

        Args:
            col_path: The collection path.
            callback: A function to execute when a callback is recieved.

        Raises:
            ValueError: If a listener to this collection/document was already registered.
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
        """Removes a listener from a collection or document.

        Args:
            listener_id: The listener id to unregister.
        """
        if listener_id in self.__listeners.keys():
            self.__listeners[listener_id].unsubscribe()
            self.__listeners.pop(listener_id)

    def disconnect(self):
        """Removes all listeners and closes the database connection."""
        for listener in self.__listeners.values():
            listener.unsubscribe()
        self.__listeners.clear()
        self.__callback_command.clear()
        self.__db.close()

    def document_ref(self, col_path: str) -> str:
        """Generates a new document id under the collection.

        Args:
            col_path: The collection under which to generate the new document id.
        Returns:
            The new document id generated under the collection
        """
        return self.__db.collection(col_path).document().id

    def map_to_object(
        self,
        dicts: Dict[str, Dict[str, Any]],
        id_field: str,
        from_dict: Callable[[Dict[str, Any]], Any],
        key_prop: str = None,
    ) -> Dict[str, Any] | List:
        """Map dicts to a dict of `object_type` values.

        Args:
            dicts: a dictionary of objects to map, id and dict pair.
            id_field: name of the identification field
            from_dict: a method to convert a database dictionary to the requested object.
                Args:
                    `Dict[str, Any]`: a dictionary with (field-name, value) pairs.
                Returns:
                    `Any`: the parsed object
            key_prop(optional): the property to be used as key in the returned dict
                (default: None).

        Returns:
            A collection of `object_type`: If `key_prop` is set returns a dict, otherwise returns a list.

        Raises:
            KeyError: if no `key_prop` property exist in object's dictionary keys.
            AttributeError: if `id_field` not set or is empty.
        """
        objects: Dict[str, Any] | List[Any] = {} if key_prop is not None else []

        for id, obj_dict in dicts.items():
            if not id_field:
                raise AttributeError(
                    "'id_field' must be set to a value.(id_field: {})".format(id_field)
                )
            if len(id_field) == 0:
                raise AttributeError(
                    "'id_field' must not be empty. (id_field: {})".format(id_field)
                )
            obj_dict[id_field] = id
            mapped_obj = from_dict(obj_dict)
            if key_prop is not None:
                objects[obj_dict[key_prop]] = mapped_obj
            else:
                objects.append(mapped_obj)

        return objects

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
    """A class describing a direction and field for ordering documents."""

    ASCENDING = 0
    DESCENDING = 1

    def __init__(self, field: str, direction: Literal = ASCENDING) -> None:
        """Create a new Order-By descriptor.

        Args:
            field: The field used to order.

            direction(optional): Direction of ordering, default: `OrderBy.ASCENDING`
                possible values: `OrderBy.ASCENDING`, `OrderBy.DESCENDING`.
        """
        self.field = field

        if direction is OrderBy.ASCENDING:
            self.direction = Query.ASCENDING
        else:
            self.direction = Query.DESCENDING


class Where(object):
    """A class describing a condition for filtering documents."""

    def __init__(self, field: str, operator: str, value: Any) -> None:
        """Create a new Where descriptor.

        Args:
            field: The field to filter on.

            operator: The comparison operation to apply on the selected field,
                possible values: ``<``, ``<=``, ``==``, ``>=``, ``>``,
                and ``in``.

            value: The value to compare the field against in the filter.
        """
        self.field = field
        self.operator = operator
        self.value = value
