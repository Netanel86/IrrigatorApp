from __future__ import annotations
from enum import Enum
import os
import sqlite3
from sqlite3 import Error, IntegrityError
from pathlib import Path
from typing import Any, Callable, Dict, List, Tuple

TYPE_TEXT = "TEXT"
TYPE_INT = "INTEGER"
TYPE_TIME = "TIMESTAMP"
TYPE_FLOAT = "FLOAT"
COL_ROWID = "ROWID"
VALUE_NULL = "IS NULL"

_TUP_COLUMNS = "col_names"
_TUP_VALUES = "values"


class SQLiteConnection(object):
    def __init__(self) -> None:
        self.__PATH = os.path.join(
            os.path.dirname(os.path.abspath(__file__)), "sqlite\db\pysqlite.db"
        )
        self.__create_db_dir()
        self.__init_db()

    def __init_db(self):
        try:
            self.__db = sqlite3.connect(
                self.__PATH,
                detect_types=sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES,
            )
        except Error as ex:
            print(ex)

    def __create_db_dir(self):
        """Creates database directory if it does not exist"""
        path_tupel = os.path.split(self.__PATH)

        Path(path_tupel[0]).mkdir(parents=True, exist_ok=True)

    def __parse_result(self, col_data: Tuple[Tuple], values: List[Tuple]):
        """Convert a `Queries.Select` query result to a list of dictionaries.

        Args:
            col_data: a list of column names.
            values: a list of values in order of there respective column name.

        Returns:
            A list of dictionaries with (key=column-name, val=value) pairs.
        """
        result: List[Dict[str, Any]] = []
        for tup_obj in values:
            dic = self.__merge_to_dict(col_data, tup_obj)
            result.append(dic)
        return result

    def __merge_to_dict(self, col_names: Tuple[Tuple], values: Tuple) -> Dict[str, Any]:
        """Merge two tuples of column-names and values to a dictionary of key, value pairs

        e.g:
            merged = (
                col_names[0]: values[0],

                col_names[1]: values[1],
                ...
            )
        Args:
            col_names: the column names in order of there respective value.
            values: the values in order of there respective column name.

        Returns:
            A dictionary with (column-name, value) pairs
        """
        dict = {}
        for (col, val) in zip(col_names, values):
            dict[col[0]] = val

        return dict

    def _execute(
        self,
        q_type: Queries,
        query: str,
        values: Tuple | List[Tuple] = None,
    ) -> bool | int | Dict[str, Any] | List[Dict[str, Any]]:
        result = None
        is_many = False
        try:
            cursor = self.__db.cursor()
            if values is not None:
                if isinstance(values, list):
                    cursor.executemany(query, values)
                    is_many = True
                else:
                    cursor.execute(query, values)
            else:
                cursor.execute(query)

            match q_type:
                case Queries.INSERT:
                    result = cursor.rowcount if is_many else cursor.fetchone()[0]
                    result = result if result is not None else cursor.lastrowid

                case Queries.SELECT:
                    result = self.__parse_result(cursor.description, cursor.fetchall())

                case Queries.CREATE | Queries.UPDATE | Queries.DELETE:
                    result = True

            cursor.close()

            if q_type == Queries.INSERT or Queries.UPDATE or Queries.DELETE:
                self.__db.commit()

        except Error as ex:
            print(ex)
            result = False
        finally:
            return result

    def _formatter(
        self, data: Tuple[str] | str, separator: str, count: int = None, **kwargs
    ) -> str:
        """Formats the given data and arguments into a single string.

        Args:
            data: a string or strings to format.
            separator: a charcter or string to be used as a separator after each element in `data`.
            count(optional): if `data` is a string, sets the number of times to repeat it
                (default: `None`).
            kwargs(optional): additional arguments to format.
                possible values:
                    'each': a string or a tuple of strings to be added after each element in `data`.
                    'last': a string to be added at the end of the final string.

        Returns:
            A formatted string.
        """
        each: str | Tuple[Any] = kwargs.get("each", "")
        last: str = kwargs.get("last", "")
        query = ""
        if isinstance(data, tuple):
            is_each_str = isinstance(each, str)
            for idx, string in enumerate(data):
                query += string + (each if is_each_str else each[idx])
                query += separator if idx < len(data) - 1 else ""
        else:
            for idx in range(count):
                query += data
                query += separator if idx < count - 1 else ""

        query += last

        return query

    def is_exists(self, table: str):
        exists = False

        ret_val = (
            self.select("sqlite_master", ("name",))
            .where(("type", "name"), ("table", table))
            .execute()
        )
        if len(ret_val) == 1:
            exists = True

        return exists

    def create(self, table: str, data: Tuple[Tuple[str]]) -> bool:
        """Create a new table in the database.

        Args:
            table: the name for the table to be created.
            data: a tuple of (column-name, type) pairs describing each column data type in the table.
                note: if 'id' (name, type) pair is set, it should be the first pair in `data`.
                default 'id' type: `int` (if not specified)

        Raises:
            ValueError: if `id` (column-name, type) pair is set, but is not the first element in `data`.

        Returns:
            `True` if the table was created successfully or already existed, `False` otherwise.
        """
        cols_query = ""
        for idx, col in enumerate(data):
            name = col[0]
            type = col[1]
            if name == "id" and idx != 0:
                raise ValueError(
                    "in {}.{}(): 'id' should be the first (index = 0) name, type pair in col_data, current 'id' index is {}".format(
                        SQLiteConnection.__name__, self.create.__name__, idx
                    )
                )

            if idx == 0:
                if name == "id":
                    cols_query += "id {} PRIMARY KEY".format(type)
                else:
                    cols_query += "id INTEGER PRIMARY KEY"
                if len(data) - 1 > 1:
                    cols_query += ", "
            else:
                cols_query += "{} {}".format(name, type)
                if idx < len(data) - 1:
                    cols_query += ", "

        query = "CREATE TABLE IF NOT EXISTS {} ({})".format(table, cols_query)

        return self._execute(Queries.CREATE, query)

    def __to_tuple_set(self, data: Dict[str, Any] | List[Dict[str, Any]]):
        tuples_dict: Dict[str, Tuple | List] = {}
        if isinstance(data, dict):
            tuples_dict[_TUP_COLUMNS] = tuple(data.keys())
            tuples_dict[_TUP_VALUES] = tuple(data.values())
        else:
            tuples_dict[_TUP_COLUMNS] = tuple(data[0].keys())
            tuples_dict[_TUP_VALUES] = []
            for val_dict in data:
                tuples_dict[_TUP_VALUES].append(tuple(val_dict.values()))
        return tuples_dict

    def insert(
        self, table: str, data: Dict[str, Any] | List[Dict[str, Any]]
    ) -> str | int:
        """Insert a new row to table.

        Args:
            table: the name for the table to be created.
            data: a dictionary of (column-name, value) pairs to insert.

        Returns:
            If successful and `values` is:
            * a single row, returns its id or rowid if id is empty.
            * multiple rows, returns the count of inserted rows.

            otherwise returns `False`
        """
        attrs = self.__to_tuple_set(data)
        col_names: Tuple = attrs[_TUP_COLUMNS]
        values: Tuple | List[Tuple] = attrs[_TUP_VALUES]

        cols_query = self._formatter(col_names, ",")
        vals_query = self._formatter("?", ",", len(col_names))

        query = "INSERT INTO {}({}) VALUES ({})".format(table, cols_query, vals_query)

        if not isinstance(values, list):
            query += " RETURNING id"

        return self._execute(Queries.INSERT, query, values)

    def update(
        self, table: str, data: Dict[str, Any] | List[Dict[str, Any]]
    ) -> QueryBuilder:
        """Update's the columns fields in the specified row id.

        Args:
            table: name of the updated table.
            data: a dictionary of (column-name, value) pairs to update.

        Returns:
            a `QueryBuilder` instance of the update query.
        """
        attr = self.__to_tuple_set(data)
        col_names = attr[_TUP_COLUMNS]
        values = attr[_TUP_VALUES]
        cols_query = self._formatter(
            col_names,
            ",",
            each=" = ?",
        )
        query = "UPDATE {} SET {}".format(table, cols_query)
        return QueryBuilder(self, Queries.UPDATE, query, values)

    def delete(self, table: str, id: str | int = None) -> bool:
        """Delete a single row or an entire table

        Args:
            table: name of table.
            id(optional): the row id to delete, if not specified deletes the entire table.
                (default: `None`).

        Returns:
            `True` if deleted successfully, `False` otherwise.
        """
        if id is None:
            query = "DROP TABLE IF EXISTS {}".format(table)
        else:
            query = "DELETE FROM {} WHERE id = ?".format(table)
        return self._execute(Queries.DELETE, query, id)

    def select(self, table: str, col_names: Tuple[str] = None) -> QueryBuilder:
        """Select columns from rows in the specified table.

        Args:
            table: name of table to select from.
            col_names: a tuple of column-names to return.

        Returns:
            a `QueryBuilder` instance of the select query.
        """
        cols_query = self._formatter(col_names, ", ") if col_names is not None else "*"
        query = "SELECT {} FROM {}".format(cols_query, table)

        return QueryBuilder(self, Queries.SELECT, query)

    def map_to_object(
        self,
        dicts: List[Dict[str, Any]],
        from_dict: Callable[[Dict[str, Any]], Any],
        key_col: str = None,
    ) -> Dict[str, Any] | List:
        """Map a list of returned database dictionaries to a dictionary of any object type

        Args:
            dicts: a list of database returned dictionaries to map.
            from_dict: a method to convert a database dictionary row to the requested object.
                Args:
                    `Dict[str, Any]`: a dictionary with (column-name, value) pairs.
                Returns:
                    `Any`: the parsed object
            key_col(optional): the column name to be used as key in the returned dict.
                default: None.

        Returns:
            A collection of objects: If `key_col` is set returns a dictionary, otherwise returns a list.

        Raises:
            KeyError: if `key_col` does not exist in `dicts` keys
        """
        objects: Dict[str, Any] | List = {} if key_col is not None else []

        for dic in dicts:
            mapped_obj = from_dict(dic)

            if key_col is not None:
                objects[dic[key_col]] = mapped_obj
            else:
                objects.append(mapped_obj)

        return objects

    def close(self):
        """Closes all database handles"""
        if self.__db is not None:
            self.__db.close()


class QueryBuilder(object):
    """A class describing a builder for adding custom clauses to queries."""

    ORDER_ASC = " ASC"
    ORDER_DESC = " DESC"

    def __init__(
        self,
        connection: SQLiteConnection,
        type: Queries,
        base_query: str,
        data: Tuple[Any] = None,
    ) -> None:
        """Creates a new builder instance.

        Args:
            connection: sqlite client.
            type: the type of query being built.
            base_query: the base string for the query.
            data(optional): initial data for `base_query`.
                (default: `None`)
        """
        self.query = base_query
        self.connection = connection
        self.__type = type
        self.__where = False
        self.__orderby = False
        self.__data: Tuple[Any] = data

    def __integrity_error(self, method: str, condition: bool, error_msg: str):
        if condition:
            raise IntegrityError(
                "in {}.{}(): query '{}': {}".format(
                    self.__class__.__name__, method, self.__type, error_msg
                )
            )

    def __format_null_values(self, values: Tuple[Any]):
        """Formats each of string\s values: each value in `values` set to `VALUE_NULL`
        will be inserted in the query string as 'IS_NULL' and not as '?'.
        """
        each: Tuple[str] = ()
        for value in values:
            if value == VALUE_NULL:
                each += (" " + VALUE_NULL,)
            else:
                each += (" = ?",)
        return each

    def where(self, col_names: Tuple[str], values: Tuple[Any]):
        """Set a condition for filtering documents.

        Args:
            col_names: a tuple of column-names to filter by.
            values: a tuple of values to filter in order of there respective column-names.

        Raises:
            IntegrityError: if:
            * more then one `WHERE` clause is set.
            * `WHERE` clause is set after an `ORDER BY` clause.

        Returns:
            a `QueryBuilder` instance of the where query.
        """
        self.__integrity_error(
            self.where.__name__,
            self.__where == True,
            "'WHERE' clause has already been set.",
        )
        self.__integrity_error(
            self.where.__name__,
            self.__orderby == True,
            "'WHERE' clause should appear before the 'ORDER BY' clause.",
        )

        each: Tuple[str] = ()
        if VALUE_NULL in values:
            each = self.__format_null_values(values)
            values = tuple(val for val in values if val != VALUE_NULL)
            values = None if len(values) == 0 else values
        else:
            each = " = ?"

        cols_query = self.connection._formatter(col_names, " AND ", each=each)

        self.query += " WHERE {}".format(cols_query)

        if self.__data == None:
            self.__data = values
        else:
            self.__data += values

        self.__where = True
        return self

    def orderby(self, col_names: Tuple[str], directions: Tuple[str] = (ORDER_ASC,)):
        """Set fields and directions for ordering documents.

        Args:
            col_names: a tuple of column-names to order by.
            directions(optional): a tuple of directions in order of there respective column-name.
                possible values: `ORDER_ASC` or `ORDER_DESC`.
                (default: `ORDER_ASC`).

        Raises:
            IntegrityError: if:
            * more then one `ORDER BY` clause is set
            * an `ORDER BY` clause is set in an `UPDATE` query

        Returns:
            a `QueryBuilder` instance of the orderby query.
        """
        self.__integrity_error(
            self.orderby.__name__,
            self.__orderby == True,
            "only a single 'ORDER BY' clause is allowed.",
        )
        self.__integrity_error(
            self.orderby.__name__,
            self.__type == Queries.UPDATE,
            "no 'ORDER BY' clause is allowed",
        )

        cols_query = self.connection._formatter(col_names, ", ", each=directions)
        self.query += " ORDER BY {}".format(cols_query)
        self.__orderby = True
        return self

    def execute(self):
        """Executes the query.

        Returns:
            The result of the executed query, If query is:
            * `Queries.SELECT`: returns a list of dictionaries of returned rows.
            * `Queries.UPDATE`: returns `True` if successful, `False` otherwise.

        Raises:
            IntegrityError: if `self.__type == Queries.UPDATE` and a `WHERE` clause has not been set.
        """

        self.__integrity_error(
            self.execute.__name__,
            self.__type == Queries.UPDATE and self.__where == False,
            "a 'WHERE' clause must be set with at least one value",
        )
        return self.connection._execute(self.__type, self.query, self.__data)


class Queries(Enum):
    CREATE = "create"
    INSERT = "insert"
    UPDATE = "update"
    SELECT = "select"
    DELETE = "delete"
