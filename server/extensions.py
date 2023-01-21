from typing import Dict, Iterable


def reverse_dict(dict: Dict):
    """Reverese dictionary `(key: value)` pairs to `(value: key)` pairs"""
    return {val: key for key, val in dict.items()}


def is_empty(iterable: Iterable):
    """Returns `True` if the iterable collection is empty."""
    return len(iterable) == 0


