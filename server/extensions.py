from typing import Any, Callable, Dict, Generic, Iterable, TypeVar


def reverseDict(dict: Dict):
    return {val: key for key, val in dict.items()}


def isEmpty(iterable: Iterable):
    return len(iterable) == 0
