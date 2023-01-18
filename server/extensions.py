from typing import Any, Callable, Dict, Generic, Iterable, TypeVar


def reverse_dict(dict: Dict):
    return {val: key for key, val in dict.items()}


def is_empty(iterable: Iterable):
    return len(iterable) == 0
