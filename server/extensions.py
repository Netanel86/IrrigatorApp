from typing import Any, Dict, Iterable, List


def reverse_dict(dict: Dict, exclude: List = None) -> Dict:
    """Reverese a dictionary with `(key: value)` pairs to `(value: key)` pairs; excluding specified keys"""
    reversed_dict = {val: key for key, val in dict.items()}
    if exclude:
        for key in exclude:
            reversed_dict.pop(key)

    return reversed_dict


def is_empty(iterable: Iterable) -> bool:
    """Returns `True` if the collection is empty."""
    return len(iterable) == 0


def get_cls_fields_values(clazz: type, exclude: List[str] = None) -> List[Any]:
    """Get a collection of class constanst fields values; excluding specified fields"""
    fields = [val for name, val in vars(clazz).items() if not name.startswith("__")]
    if exclude:
        for field in exclude:
            fields.remove(field)

    return fields
