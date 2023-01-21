from __future__ import annotations
from collections import namedtuple
from enum import Enum
import os
import sqlite3
from extensions import is_empty
from sqlite3 import Error, IntegrityError
from pathlib import Path
from typing import Any, Callable, Dict, List, Tuple
from infra import Loggable

__ValueTypes = namedtuple("__ValueTypes", "TEXT INT TIME FLOAT")

TYPES = __ValueTypes("TEXT", "INTEGER", "TIMESTAMP", "FLOAT")
"""A list of sqlite supported database types"""

__Parsers = namedtuple("__Parsers", "DICT TUPLE")
PARSE = __Parsers(0, 1)

COL_ROWID = "ROWID"
VALUE_NULL = "IS NULL"

_TUP_COLUMNS = 0
_TUP_VALUES = 1


class SQLiteConnection(Loggable):
    def __init__(self) -> None:
        self.__PATH = os.path.join(
            os.path.dirname(os.path.abspath(__file__).split("data")[0]),
            "sqlite\db\pysqlite.db",
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
            method_sig = self.__init_db.__name__.removeprefix("__")
            self.logger.error(f"{method_sig}> {ex}")

    def __create_db_dir(self):
        """Creates database directory if it does not exist"""
        path_tupel = os.path.split(self.__PATH)

        Path(path_tupel[0]).mkdir(parents=True, exist_ok=True)

    def __parse_result(
        self,
        col_data: Tuple[Tuple],
        values: List[Tuple],
        merger: int,
    ):
        """Parse a `Queries.Select` query result to a list using a merger method.

        Args:
            * `col_data`: a list of column names.
            * `values`: a list of values in order of there respective column names.
            * `merger`: the id for the parser type to be used.
                * possible values: `PARSE.DICT` and `PARSE.TUPLE`, default: `PARSE.TUPLE`

        Returns:
            * If `merger` is:
                * `PARSE.DICT`: A list of dictionaries with (column-name: value) pairs.
                * `PARSE.TUPLE`: A list of tuples with (column-name, value) pairs.
        """
        merge: Callable[[Tuple[Tuple], List[Tuple]], Any] = (
            self.merge_to_tuple
            if merger is None or merger == PARSE.TUPLE
            else self.merge_to_dict
        )
        result: List[Tuple[Tuple]] = []
        for tup_obj in values:
            result.append(merge(col_data, tup_obj))
        return result

    def __to_tuple_set(self, data: Dict[str, Any] | List[Dict[str, Any]]):
        tuples_list: List[Tuple | List] = []
        if isinstance(data, dict):
            tuples_list.append(tuple(data.keys()))
            tuples_list.append(tuple(data.values()))
        else:
            tuples_list.append(tuple(data[0].keys()))
            tuples_list.append([])
            for val_dict in data:
                tuples_list[_TUP_VALUES].append(tuple(val_dict.values()))
        return tuples_list

    def _execute(
        self,
        q_type: Queries,
        query: str,
        values: Tuple | List[Tuple] = None,
        result_parser: Callable[[Tuple[Tuple], List[Tuple]], Any] = None,
    ) -> bool | int | Dict[str, Any] | List[Dict[str, Any]]:
        method_sig = self._execute.__name__.removeprefix("_")
        self.logger.info(f"{method_sig}> {q_type.name}: {query}")
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
                    if isinstance(result_parser, Callable):
                        result = result_parser(cursor.description, cursor.fetchall())
                    else:
                        result = self.__parse_result(
                            cursor.description,
                            cursor.fetchall(),
                            result_parser,
                        )

                case Queries.CREATE | Queries.UPDATE | Queries.DELETE:
                    result = True

            cursor.close()

            if q_type == Queries.INSERT or Queries.UPDATE or Queries.DELETE:
                self.__db.commit()

        except Exception as ex:
            self.logger.warning(f"{method_sig}> {ex}")
            result = False
        finally:
            return result

    def _formatter(
        self, data: Tuple[str] | str, separator: str, count: int = None, **kwargs
    ) -> str:
        """Formats the given data and arguments into a single string.

        Args:
            * `data`: a string or strings to format.
            * `separator`: a charcter or string to be used as a separator after each element in `data`.
            * `count`(optional): if `data` is a string, sets the number of times to repeat it,
                default: `None`
            * `kwargs`(optional): additional arguments to format.
                * possible values:
                    * `suffix`- `Tuple[str] | str`: to be added after each element in `data`.
                    * `prefix`- `Tuple[str] | str`: to be added before each element in `data`.

        Returns:
            A formatted string.
        """
        suffix: str | Tuple[Any] = kwargs.get("suffix", "")
        prefix: str | Tuple[Any] = kwargs.get("prefix", "")
        query = ""
        if isinstance(data, tuple):
            is_suf_str = isinstance(suffix, str)
            is_pre_str = isinstance(prefix, str)
            for idx, string in enumerate(data):
                query += (
                    (prefix if is_pre_str else prefix[idx])
                    + string
                    + (suffix if is_suf_str else suffix[idx])
                )
                query += separator if idx < len(data) - 1 else ""
        else:
            for idx in range(count):
                query += data
                query += separator if idx < count - 1 else ""

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
            * `table`: the name for the table to be created.
            * `data`: a tuple of (column-name, type) pairs describing each column data type in the table.
                * note: if 'id' (name, type) pair is set, it should be the first pair in `data`.
                default 'id' type: `int` (if not specified)

        Raises:
            `ValueError`: if `id` (column-name, type) pair is set, but is not the first element in `data`.

        Returns:
            `True` if the table was created successfully or already existed, `False` otherwise.
        """
        cols_query = ""
        for idx, col in enumerate(data):
            name = col[0]
            type = col[1]
            if name == "id" and idx != 0:
                raise ValueError(
                    f"in {SQLiteConnection.__name__}.{self.create.__name__}(): 'id' should be the first (index = 0) name, type pair in col_data, current 'id' index is {idx}"
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

    def insert(
        self, table: str, data: Dict[str, Any] | List[Dict[str, Any]]
    ) -> str | int:
        """Insert a new row to table.

        Args:
            * `table`: the table name to insert the new row in.
            * `data`: a dictionary of (column-name, value) pairs to insert.

        Returns:
            * If successful and `data` is:
                * a single row, returns its id or rowid if id is empty.
                * multiple rows, returns the count of inserted rows.
            * If failed returns `False`
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
            * `table`: name of the updated table.
            * `data`: a dictionary of (column-name, value) pairs to update.

        Returns:
            a `QueryBuilder` instance of the update query.
        """
        attr = self.__to_tuple_set(data)
        col_names = attr[_TUP_COLUMNS]
        values = attr[_TUP_VALUES]
        cols_query = self._formatter(
            col_names,
            ",",
            suffix=" = ?",
        )
        query = "UPDATE {} SET {}".format(table, cols_query)
        return QueryBuilder(
            self, Queries.UPDATE, query, col_names=col_names, data=values
        )

    def delete(self, table: str, id: str | int = None) -> bool:
        """Delete a single row or an entire table

        Args:
            * `table`: name of table.
            * `id`(optional): the row id to delete, if not specified deletes the entire table.
                default: `None`.

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
            * `table`: name of table to select from.
            * `col_names`: a tuple of column-names to return.

        Returns:
            a `QueryBuilder` instance of the select query.
        """
        cols_query = self._formatter(col_names, ", ") if col_names is not None else "*"
        query = "SELECT {} FROM {}".format(cols_query, table)

        return QueryBuilder(
            self, Queries.SELECT, query, table=table, col_names=col_names
        )

    def merge_to_dict(self, col_names: Tuple[Tuple], values: Tuple) -> Dict[str, Any]:
        """Merge two tuples of column-names and values to a dictionary.

        Args:
            * `col_names`: the column names in order of there respective value.
            * `values`: the values in order of there respective column name.

        Returns:
            A dictionary with (column-name: value) pairs
        """
        return {col[0]: val for (col, val) in zip(col_names, values)}

    def merge_to_tuple(self, col_names: Tuple[Tuple], values: Tuple) -> List[Tuple]:
        """Merge two tuples of column-names and values to a single tuple of tuple pairs.

        Args:
            * `col_names`: the column names in order of their respective values.
            * `values`: the values in order of their respective column names.

        Returns:
            A tuple with (column-name, value) tuple pairs
        """
        return tuple([(col[0], val) for (col, val) in zip(col_names, values)])

    def map_to_object(
        self,
        dicts: List[Dict[str, Any]],
        from_dict: Callable[[Dict[str, Any]], Any],
        key_col: str = None,
    ) -> Dict[str, Any] | List:
        """Map a list of returned database dictionaries to a dictionary of any object type

        Args:
            * `dicts`: a list of database returned dictionaries to map.
            * `from_dict`: a method to convert a database dictionary row to the requested object.
                * Args:
                    * `Dict[str, Any]`: a dictionary with (column-name, value) pairs.
                * Returns:
                    * `Any`: the parsed object
            * `key_col`(optional): the column name to be used as key in the returned dict.
                default: `None`.

        Returns:
            A collection of objects: If `key_col` is set returns a dictionary, otherwise returns a list.

        Raises:
            `KeyError`: if `key_col` does not exist in `dicts` keys
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
    """A class describing a modular builder for queries."""

    __Orders = namedtuple("__Orders", "ASCENDING DESCENDING")
    ORDER = __Orders(" ASC", " DESC")

    __Joins = namedtuple("__Joins", "INNER LEFT CROSS")
    JOIN = __Joins("INNER", "LEFT", "CROSS")

    __SubQueries = namedtuple("__SubQueries", "WHERE ORDERBY JOIN")
    __SUBQR = __SubQueries("WHERE", "ORDER BY", "JOIN")

    def __init__(
        self, connection: SQLiteConnection, type: Queries, base_query: str, **kwargs
    ) -> None:
        """Creates a new builder instance.

        Args:
            * `connection`: sqlite client.
            * `type`: the type of query being built.
            * `base_query`: the base string for the query.
            * `kwargs`: additional argument to pass.
                * possible values:
                    * `table`- `str`: name of the base table in the query.
                    * `col_names`- `Tuple[str]`: list of the selected columns in the query.
                    * `data`- `Tuple`: list of initial data for `base_query`.
        """
        data: Tuple = kwargs.get("data", None)
        table: str = kwargs.get("table", None)
        col_names: Tuple[str] = kwargs.get("col_names", None)
        self.query = base_query
        self.connection = connection
        self.__type = type
        self.__where = False
        self.__orderby = False
        self.__join = False
        self.__data: Tuple[Any] = data
        self.__tables: List[str] = [table] if table else []
        self.__cols: List[Tuple[str]] = [col_names] if col_names else []

    def __integrity_error(
        self, clause: str, method: str, condition: bool, error_msg: str
    ):
        if condition:
            raise IntegrityError(
                "in {}.{}(): query '{}': {}: '{}".format(
                    self.__class__.__name__, method, self.__type, error_msg, clause
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
            * `col_names`: a tuple of column-names to filter by.
            * `values`: a tuple of values to filter in order of there respective column-names.

        Raises:
            * `IntegrityError` if:
                * more then one `WHERE` clause is set.
                * `WHERE` clause is set after an `ORDER BY` clause.

        Returns:
            a `QueryBuilder` instance of the where query.
        """
        self.__integrity_error(
            QueryBuilder.__SUBQR.WHERE,
            self.where.__name__,
            self.__where == True,
            "Can't set clause more then once",
        )
        self.__integrity_error(
            QueryBuilder.__SUBQR.WHERE,
            self.where.__name__,
            self.__orderby == True,
            "Can't set clause after '{}'".format(QueryBuilder.__SUBQR.ORDERBY),
        )

        each: Tuple[str] = ()
        if VALUE_NULL in values:
            each = self.__format_null_values(values)
            values = tuple(val for val in values if val != VALUE_NULL)
            values = None if len(values) == 0 else values
        else:
            each = " = ?"

        cols_query = self.connection._formatter(col_names, " AND ", suffix=each)

        self.query += " WHERE {}".format(cols_query)

        if len(col_names) != len(values):
            is_multi = True if isinstance(values[0], tuple) else False
            for idx in range(len(self.__data)):
                self.__data[idx] = self.__data[idx] + (
                    values[idx] if is_multi else (values[idx],)
                )

        elif self.__data == None:
            self.__data = values
        else:
            self.__data += values

        self.__where = True
        return self

    def orderby(
        self, col_names: Tuple[str], directions: Tuple[str] = (ORDER.ASCENDING,)
    ):
        """Set fields and directions for ordering documents.

        Args:
            * `col_names`: a tuple of column-names to order by.
            * `directions`(optional): a tuple of directions in order of there respective column-name.
                * possible values: `ORDER.ASCENDING` or `ORDER.DESCENDING`, default: `ORDER.ASCENDING`.

        Raises:
            * `IntegrityError` if:
                * more then one `ORDER BY` clause is set
                * `ORDER BY` clause is set in an `UPDATE` query

        Returns:
            a `QueryBuilder` instance of the orderby query.
        """
        self.__integrity_error(
            QueryBuilder.__SUBQR.ORDERBY,
            self.orderby.__name__,
            self.__orderby == True,
            "Can't set clause more then once",
        )
        self.__integrity_error(
            QueryBuilder.__SUBQR.ORDERBY,
            self.orderby.__name__,
            self.__type == Queries.UPDATE,
            "clause is not allowed",
        )

        cols_query = self.connection._formatter(col_names, ", ", suffix=directions)
        self.query += " ORDER BY {}".format(cols_query)
        self.__orderby = True
        return self

    def join(self, table: str, col_names: Tuple[str] = None, type: str = "", **kwargs):
        """Join data from an additional table to the query

        Args:
            * `table`: name of table to select from.
            * `col_names`: a tuple of column-names to select.
            * `type`(optional): join type.
                * possible values: `JOIN.INNER`, `JOIN.LEFT` and `JOIN.CROSS`
                default: `None`.
            * `kwargs`(optional): additional filtering conditions.
                * possible values:
                    * `src_col`: column name from base table to compare
                    * `target_col`: column name from joined table to compare,
                        if both columns names are the same set only `src_col`

        Returns:
            a `QueryBuilder` instance of the join query.

        Raises:
            * `IntegrityError` if:
                * more then one `JOIN` clause is set
                * `JOIN` clause is set after a `WHERE` clause
                * `JOIN` clause is set after a `ORDER BY` clause
        """
        self.__integrity_error(
            QueryBuilder.__SUBQR.JOIN,
            self.join.__name__,
            self.__join == True,
            "Can't set clause more then once",
        )
        self.__integrity_error(
            QueryBuilder.__SUBQR.JOIN,
            self.join.__name__,
            self.__where == True,
            "Can't set clause after '{}'".format(QueryBuilder.__SUBQR.WHERE),
        )
        self.__integrity_error(
            QueryBuilder.__SUBQR.JOIN,
            self.join.__name__,
            self.__orderby == True,
            "Can't set clause after '{}'".format(QueryBuilder.__SUBQR.ORDERBY),
        )

        cols_query_base = (
            self.connection._formatter(
                self.__cols[0], ", ", prefix=self.__tables[0] + "."
            )
            if not is_empty(self.__cols)
            else self.__tables[0] + ".*"
        )

        cols_query_join = self.connection._formatter(
            col_names, ", ", prefix=table + "."
        )

        self.query = "SELECT {}, {} FROM {}".format(
            cols_query_base, cols_query_join, self.__tables[0]
        )

        src_col = kwargs.get("src_col", None)
        target_col = kwargs.get("target_col", None)

        type_str = (" " + type) if not is_empty(type) else ""
        join_data = ""

        if src_col is not None:
            target_col = src_col if target_col is None else target_col
            join_data = " ON {}.{} = {}.{}".format(
                self.__tables[0], src_col, table, target_col
            )

        self.query += type_str + " JOIN {}{}".format(table, join_data)
        self.__tables.append(table)
        self.__join == True
        return self

    def execute(
        self, result_parser: Callable[[Tuple[Tuple], List[Tuple]], Any] | int = None
    ):
        """Executes the query.

        Args:
            * `result_parser`(optional)- `int`: the id for the parser type to be used.
                * possible values: `PARSE.TUPLE` and `PARSE.DICT`, default: `PARSE.TUPLE`
                    * OR
            * `result_parser`(optional)- `Callable[[Tuple[Tuple], List[Tuple]]`: a custom method to parse the database result.
                    * Args:
                        * `col_data`- `Tuple[Tuple]`: the result columns data
                        * `values`- `List[Tuple]`: the result values
                    * Returns:
                        * `Any`: the parsed data

        Returns:
            * The result of the executed query, If query is:
                * `Queries.SELECT`: returns a tuple set of returned rows (if `result_parser` is default).
                * `Queries.UPDATE`: returns `True` if successful, `False` otherwise.

        Raises:
            `IntegrityError`: if `WHERE` clause has not been set in an `Queries.UPDATE` query.
        """

        self.__integrity_error(
            QueryBuilder.__SUBQR.WHERE,
            self.execute.__name__,
            self.__type == Queries.UPDATE and self.__where == False,
            "Missing clause",
        )
        return self.connection._execute(
            self.__type, self.query, self.__data, result_parser
        )


class Queries(Enum):
    CREATE = "create"
    INSERT = "insert"
    UPDATE = "update"
    SELECT = "select"
    DELETE = "delete"
