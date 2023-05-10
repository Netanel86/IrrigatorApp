from __future__ import annotations
from abc import ABC
from enum import Enum
import logging
from datetime import datetime
from google.api_core.datetime_helpers import DatetimeWithNanoseconds
from typing import Any, Dict, List, NamedTuple, Tuple, Callable
from extensions import is_empty


class IDable(ABC):
    @property
    def id(self) -> int:
        raise NotImplementedError(
            f"'id': property getter not implemented in class '{self.__class__.__name__}'"
        )

    @id.setter
    def id(self, value: int):
        raise NotImplementedError(
            f"'id': property setter not implemented in class '{self.__class__.__name__}'"
        )


class Logger(logging.Logger):
    def __init__(self, name) -> None:
        super().__init__(name)
        self.propagate = False
        formatter = logging.Formatter(
            "%(levelname)s %(asctime)s %(name)s - %(message)s", "%X"
        )
        handler = logging.StreamHandler()
        handler.setFormatter(formatter)
        self.addHandler(handler)


class Observable(ABC):
    def __init__(self) -> None:
        self._callbacks: List[Callable[[Observable, str, Any, Any], None]] = None

    def register_callback(self, callback: Callable[[Observable, str, Any, Any], None]):
        if self._callbacks is None:
            self._callbacks = []
        self._callbacks.append(callback)

    def unregister_callback(
        self, callback: Callable[[Observable, str, Any, Any], None]
    ):
        self._callbacks.remove(callback)
        if is_empty(self._callbacks):
            self.clear_callbacks()

    def clear_callbacks(self):
        self._callbacks.clear()
        self._callbacks = None

    def _notify_change(self, property: str, old_value, new_value):
        if old_value != new_value:
            if self._callbacks is not None:
                for callback in self._callbacks:
                    callback(self, property, old_value, new_value)


class DictParseable(ABC):
    def __init__(self) -> None:
        super().__init__()
        self.logger = logging.getLogger(self.__class__.__name__)

    @classmethod
    def Props(cls) -> NamedTuple:
        """An abstract representation of class property names.

        To be implemented and initialized with a list of derived class property names.
        """
        raise NotImplementedError(
            f"property getter not implemented in class {cls.__class__.__name__}.Props()"
        )

    @classmethod
    def from_dict(
        cls, source: Dict[str, Any], from_map: Dict[str, str] = None
    ) -> DictParseable:
        """Parses and creates a new object from a dictionary.

        Args:
            source: a dictionary with object's properties (name: value) pairs, with to initiate the new object.
                possible values: included in `DictParseable.Props()`.

            from_map(optional): a dictionary mapping the source properties to this class properties,
                (src_prop_x: object_prop_x) pairs. use when the source and object propery names are not identical.
                default: None.

        Returns:
            `DictParseable`: the child object derived from `DictParseable`, initialized with dictionary data.

        Raises:
            `AttributeError`: if object does not contain a property name.
        """
        module = cls()
        module.update_dict(source, from_map)
        return module

    def to_dict(
        self, props: Tuple[str] = None, to_map: Dict[str, str] = None
    ) -> Dict[str, Any]:
        """Parses the 'DictParseable' object to a dictionary.

        Args:
            * `props`(optional) -- a list of properties names to parse, if set only the specified properties would be parsed,
                * possible values: included in `DictParseable.Props()`, default: `None`.
            * `from_map`(optional) -- a dictionary mapping this class properties to a set of custom properties,
                (cls_prop_x: trg_prop_x) pairs. use when custom property names are needed.
                default: None.

        Returns:
            A dictionary with the object's properties, (name: value) pairs

        Raises:
            `AttributeError`: if object does not contain one of the property names in `props` or `from_map`.
        """
        prop_dict = {}
        is_map = to_map is not None
        is_props = props is not None

        collection = props if is_props else to_map.keys() if is_map else None

        if collection is not None:
            for prop in collection:
                if not hasattr(self, prop):
                    raise AttributeError(name=prop, obj=self)

                if is_map and prop not in to_map.keys():
                    raise KeyError(f"Key: Dict 'to_map' has no key '{prop}'")
                prop_key = to_map[prop] if is_map else prop
                attr_val = getattr(self, prop)
                prop_dict[prop_key] = (
                    attr_val.name if isinstance(attr_val, Enum) else attr_val
                )
        else:
            prop_dict = self.__to_dict()

        return prop_dict

    def update_dict(self, source: Dict[str, Any], from_map: Dict[str, str] = None):
        """Updates the object with data from a dictionary.

        Args:
            * `source` -- a dictionary with object's properties (name: value) pairs, with to update the object.
                possible values: included in `DictParseable.Props()`.
            * `from_map`(optional) -- a dictionary mapping the source properties to this class properties,
                (src_prop_x: object_prop_x) pairs. use when the source and object properties names are not identical.
                default: None.

        Raises:
            `AttributeError`: if object does not contain one of the property names in `props` or `from_map`.
        """
        is_map = from_map is not None
        for prop, value in source.items():
            is_in_map = is_map and prop in from_map.keys()
            if is_map and not is_in_map:
                self.logger.warning(
                    f"Key: Dict 'from_map' has no key '{prop}', trying to use 'source' key instead.."
                )
            obj_prop = prop if (not is_map) | (not is_in_map) else from_map[prop]

            if not hasattr(self, obj_prop):
                raise AttributeError(name=obj_prop, obj=self)
                # self.__class__._raise_AttributeError(setattr.__name__, obj_prop)

            value_type = type(value)
            attr_type = type(getattr(self, obj_prop))

            if value_type is not attr_type:
                if isinstance(getattr(self, obj_prop), datetime):
                    if isinstance(value, str):
                        value = datetime.fromisoformat(value)
                    elif isinstance(value, DatetimeWithNanoseconds):
                        value = datetime.fromtimestamp(value.timestamp()).astimezone()

                elif isinstance(getattr(self, obj_prop), Enum):
                    value = attr_type[f"{value}"]

            setattr(self, obj_prop, value)

    def __to_dict(self) -> Dict[str, Any]:
        """Parses the 'DictParseable' object to a dictionary.

        Returns:
            A dictionary with the object properties, (name: value) pairs"""
        obj_dict: Dict[str, Any] = {}
        for prop in self.__class__.Props():
            attr_val = getattr(self, prop)
            if not isinstance(attr_val, list):
                obj_dict[prop] = (
                    attr_val.name if isinstance(attr_val, Enum) else attr_val
                )
        return obj_dict

    def __eq__(self, other: object):
        is_equal = False
        if isinstance(other, self.__class__):
            is_equal = self.__dict__ == other.__dict__
        else:
            raise TypeError(self.__class__, other.__class__)

        return is_equal
