from __future__ import annotations
from collections import namedtuple
from enum import Enum
import logging
import os
import sqlite3
from extensions import is_empty
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional, Tuple, Union


class Queries(Enum):
    CREATE = "create"
    INSERT = "insert"
    UPDATE = "update"
    SELECT = "select"
    DELETE = "delete"


class Parsers(Enum):
    Tuple = 0
    Dict = 1


class ValueType:
    """SQLite supported value types"""

    NONE = "NULL"
    STR = "TEXT"
    INT = "INTEGER"
    TIME = "TIMESTAMP"
    FLOAT = "REAL"


class Attribute:
    """Represents SQLite column attributes."""

    class ForeignKey:
        """Represents a foreign key and its behavior."""

        class Action:
            """Possible actions for a foreign key."""

            class ON:
                """Queries to execute an action on a foreign key."""

                DELETE = "DELETE"
                UPDATE = "UPDATE"

            SET_NULL = "SET NULL"
            SET_DEFAULT = "SET DEFAULT"
            RESTRICT = "RESTRICT"
            NO_ACTION = "NO ACTION"
            CASCADE = "CASCADE"

        def __init__(
            self, table: str, column: str, actions: Tuple[Tuple[str, str]]
        ) -> None:
            """Initializes a new `ForeignKey` object.

            Args:
                table: The name of the foreign table.
                column: The name of the foreign column to bind to.
                actions: A tuple of actions to execute when the foreign key updates.
                    Each action should be a two-value tuple, the query to execute and the action type.
                    Possible values for queries are available in `Attribute.ForeignKey.Action.ON`.
                    Possible values for actions are available in `Attribute.ForeignKey.Action`.
            """
            self.table: str = table
            self.column: str = column
            self.actions: Tuple[Tuple[str, str]] = actions

    PRIMARY_KEY = "PRIMARY KEY"
    NOT_NULL = "NOT NULL"


_Operators = namedtuple("_Operators", "IS_NULL")
OPR = _Operators("IS NULL")

COL_ROWID = "ROWID"
VALUE_NULL = "IS NULL"


_TUP_COLUMNS = 0
_TUP_VALUES = 1


class SQLiteConnection(object):
    def __init__(self, db_path, **kwargs) -> None:
        self.logger = logging.getLogger(self.__class__.__name__)
        self._db_path = db_path

        self.__create_db_dir()
        self.__init_db()

        self.__init_args(**kwargs)

    def __init_args(self, **kwargs):
        enable_foreign_keys: bool = kwargs.get("foreign_keys", False)
        if enable_foreign_keys:
            self.__db.execute("PRAGMA foreign_keys = ON")
            self.__db.commit()

    def __init_db(self):
        try:
            self.__db = sqlite3.connect(
                self._db_path,
                detect_types=sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES,
            )

        except sqlite3.Error as ex:
            method_sig = self.__init_db.__name__.removeprefix("__")
            self.logger.error(f"{method_sig}> {ex}")

    def __create_db_dir(self):
        """Creates the database directory if it does not exist"""
        method_sig = self.__create_db_dir.__name__.removeprefix("__")
        path_tupel = os.path.split(self._db_path)

        Path(path_tupel[0]).mkdir(parents=True, exist_ok=True)

        self.logger.info(f"{method_sig}> Database file path: {path_tupel[0]}")

    def __parse_result(
        self,
        col_data: Tuple[Tuple],
        values: List[Tuple],
        merger: Parsers,
    ):
        """Parse a `Queries.Select` query result to a list using a merger method.

        Args:
            * `col_data`: a list of column names.
            * `values`: a list of values in order of there respective column names.
            * `merger`: the id for the parser type to be used.
                * possible values: `Parsers.Dict` and `Parsers.Tuple`, default: `Parsers.Tuple`

        Returns:
            * If `merger` is:
                * `Parsers.Dict`: A list of dictionaries with (column-name: value) pairs.
                * `Parsers.Tuple`: A list of tuples with (column-name, value) pairs.
        """
        merge: Callable[[Tuple[Tuple], List[Tuple]], Any] = (
            self.merge_to_dict if merger == Parsers.Dict else self.merge_to_tuple
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
        result_parser: Callable[[Tuple[Tuple], List[Tuple]], Any]
        | Parsers = Parsers.Tuple,
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
                    result = cursor.fetchone()
                    result = (
                        cursor.rowcount
                        if is_many
                        else (result[0] if result is not None else result)
                    )
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

        except sqlite3.Error as ex:
            self.logger.error(f"{method_sig}> {ex}")
            result = False
        finally:
            return result

    def _formatter(
        self,
        data: Tuple[str] | str,
        separator: str,
        count: Optional[int],
        **kwargs: Optional[Dict[str, str]],
    ) -> str:
        """Formats the given data and arguments into a single string.

        Args:
            data (`Tuple[str] | str`):
                A string or a tuple of strings to format.
            separator (`str`):
                A character or string to be used as a separator after each element in `data`.
            count (`Optional[int]`):
                If `data` is a single string, sets the number of times to repeat it.
                Default is `None`.
            **kwargs (`Optional[Dict[str,str]]`):
                Additional arguments to format.
                Possible values:
                - `suffix` (`str`): A list of strings or a single string to be added after each element in `data`.
                - `prefix` (`str`): A list of strings or a single string to be added before each element in `data`.

        Returns:
            A formatted string.
        """
        suffix: str | Tuple[Any] = kwargs.get("suffix", "")
        prefix: str | Tuple[Any] = kwargs.get("prefix", "")
        query = ""
        if isinstance(data, Tuple):
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

    def create(
        self, table: str, data: Tuple[Tuple[str, str, str | Attribute.ForeignKey]]
    ) -> bool:
        """Create a new table in the database.

        Args:
            table (`str`):
                The name for the table to be created.
            data (`Tuple[Tuple[str, str, str | Attribute.ForeignKey]]`):
                A tuple of (name, type, attributes...) describing name, type and attributes for each column in the table.
                The first element of each tuple should be the column name, the second element should be the column type,
                and any additional elements should be strings representing column attributes.
                The possible attributes are defined in the `Attribute` class.

                Possible values for column attributes:
                * `Attribute.PRIMARY_KEY`: Indicates that the column is a primary key.
                * `Attribute.NOT_NULL`: Indicates that the column cannot have null values.
                * `Attribute.ForeignKey`: Indicates that the column is a foreign key. If this attribute is specified, the
                column type should be a string specifying the type of the foreign key column in the referenced table,
                and the attributes should be an `Attribute.ForeignKey` object.

        Returns:
            `bool`: `True` if the table was created successfully or already existed, `False` otherwise.
        """

        cols_query = ""
        foreign_keys = []
        for idx, col in enumerate(data):
            name = col[0]
            type = col[1]
            attrs = col[2 : len(col)]

            cols_query += f"{name} {type}"
            if not is_empty(attrs):
                bare_attrs = []
                for attr in attrs:
                    if isinstance(attr, Attribute.ForeignKey):
                        foreign_key_query = f"FOREIGN KEY ({name}) REFERENCES {attr.table}({attr.column})"
                        for action_tup in attr.actions:
                            on_query = action_tup[0]
                            action = action_tup[1]
                            foreign_key_query += f" ON {on_query} {action}"
                        foreign_keys.append(foreign_key_query)
                    else:
                        bare_attrs.append(attr)
                cols_query += f" {self._formatter(tuple(bare_attrs), ' ')}"

            if idx < len(data) - 1:
                cols_query += ", "

        foreign_query = (
            (", " + self._formatter(tuple(foreign_keys), " "))
            if not is_empty(foreign_keys)
            else ""
        )
        query = f"CREATE TABLE IF NOT EXISTS {table} ({cols_query}{foreign_query})"

        return self._execute(Queries.CREATE, query)

    def insert(
        self,
        table: str,
        data: Dict[str, Any] | List[Dict[str, Any]],
        return_col: Optional[str],
    ) -> str | int:
        """Insert one or more rows into a table.

        Args:
            table (`str`):
                The name of the table to insert into.
            data (`Dict[str, Any] | List[Dict[str, Any]]`):
                A dictionary or list of dictionaries of (column-name, value) pairs to insert.
            return_col (`Optional[str]`):
                The name of a column whose value to return after insertion. Only used when inserting a single row. Default is `None`.

        Returns:
            `(bool | int | str)`: If the insertion was successful:
                - If `data` is a single row and `return_col` is defined, returns the value of the specified column.
                - If `data` is a single row and `return_col` is `None`, returns the id or rowid of the inserted row as a string or integer.
                - If `data` is multiple rows, returns the count of inserted rows as an integer.
            If the insertion failed, returns `False`.
        """
        attrs = self.__to_tuple_set(data)
        col_names: Tuple = attrs[_TUP_COLUMNS]
        values: Tuple | List[Tuple] = attrs[_TUP_VALUES]

        cols_query = self._formatter(col_names, ",")
        vals_query = self._formatter("?", ",", len(col_names))

        query = f"INSERT INTO {table}({cols_query}) VALUES ({vals_query})"

        if return_col is not None:
            if not isinstance(data, list):
                query += f" RETURNING {return_col}"
            else:
                self.logger.warning(
                    f"{self.insert.__name__}> Return columns function is not available when inserting multiple rows, ignoring return column."
                )

        return self._execute(Queries.INSERT, query, values)

    def update(
        self, table: str, data: Dict[str, Any] | List[Dict[str, Any]]
    ) -> QueryBuilder:
        """Updates the specified row or rows in the table.

        Args:
            table (`str`): The name of the table to update.
            data (`Dict[str, Any] | List[Dict[str, Any]]`): A dictionary or list of dictionaries of (column-name, value) pairs to update.
                If a dictionary is passed, it is used to update a single row.
                If a list of dictionaries is passed, each dictionary corresponds to a row to update.

        Returns:
            A `QueryBuilder` instance representing the update query.
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

    def delete(
        self, table: str, col_names: Optional[Tuple[str, ...]], values: Optional[Tuple]
    ) -> bool:
        """Delete one or more rows from the table.

        If `col_names` and `values` are specified, delete all rows where the columns `col_names` equals the given `values`.
        If `col_names` and `values` are not specified, delete the entire table.

        Args:
            table (`str`): The name of the table to delete from.
            col_names (`Optional[Tuple[str, ...]]`): The names of the columns to filter the delete query by. Default `None`.
            values (`Optional[Tuple[Any, ...]]`): The values to filter the delete query by. Default `None`.

        Returns:
            `True` if the rows were deleted successfully, `False` otherwise.
        """
        if values and col_names:
            query = f"DELETE FROM {table}"
        else:
            query = f"DROP TABLE IF EXISTS {table}"

        return (
            QueryBuilder(self, Queries.DELETE, query, table=table)
            .where(col_names, values)
            .execute()
            if col_names and values
            else self._execute(Queries.DELETE, query, values)
        )

    def select(self, table: str, col_names: Optional[Tuple[str]]) -> QueryBuilder:
        """Select columns from rows in the specified table.

        Args:
            table (`str`): the name of the table to select from.
            col_names (`Optional[Tuple[str]]`): a tuple of column names to return. Default None.

        Returns:
            A `QueryBuilder` instance of the select query.
        """
        cols_query = self._formatter(col_names, ", ") if col_names is not None else "*"
        query = f"SELECT {cols_query} FROM {table}"

        return QueryBuilder(
            self, Queries.SELECT, query, table=table, col_names=col_names
        )

    def merge_to_dict(
        self, col_names: Tuple[Tuple], values: Tuple[Any, ...]
    ) -> Dict[str, Any]:
        """Merge two tuples of column-names and values to a dictionary.

        Args:
            col_names (`Tuple[Tuple]`): The column names in order of their respective value.
            values (`Tuple[Any, ...]`): The values in order of their respective column name.

        Returns:
            A dictionary with (column-name: value) pairs.
        """
        return {col[0]: val for (col, val) in zip(col_names, values)}

    def merge_to_tuple(
        self, col_names: Tuple[Tuple], values: Tuple[Any, ...]
    ) -> Tuple[Tuple]:
        """Merge two tuples of column-names and values to a single tuple of tuple pairs.

        Args:
            col_names (`Tuple[Tuple]`): The column names in order of their respective values.
            values (`Tuple[Any, ...]`): The values in order of their respective column names.

        Returns:
            A tuple of (column-name, value) tuple pairs.
        """
        return tuple([(col[0], val) for (col, val) in zip(col_names, values)])

    def map_to_object(
        self,
        dicts: List[Dict[str, Any]],
        from_dict: Callable[[Dict[str, Any]], Any],
        key_col: str = None,
    ) -> Dict[str, Any] | List:
        """Map a list of returned database dictionaries to a collection of any object type

        Args:
            * `dicts`: a list of database returned dictionaries to map.
            * `from_dict`: a method to convert a database dictionary row to the requested object.
                * Args:
                    * `Dict[str, Any]`: a dictionary with (column-name, value) pairs.
                * Returns:
                    * `Any`: the parsed object
            * `key_col`(optional): the column name to be used as key in the returned dictionary.
                default: `None`.

        Returns:
            A dictionary If a key column was set, otherwise a list.

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
    """Query builder to create complex queries."""

    class WhereBuilder(object):
        class CompareOP(Enum):
            EqualTo = "="
            NotEqualTo = "!="
            LessThan = "<"
            GreaterThen = ">"
            LessOrEqualTo = "<="
            GreaterOrEqualTo = ">="

        class LogicalOP(Enum):
            AND = "AND"
            OR = "OR"

        def __init__(
            self, parent_bulder: QueryBuilder, connection: SQLiteConnection
        ) -> None:
            self._connection = connection
            self._parent_builder = parent_bulder
            self._query = ""
            self._expect_logical = False

        def compare(self, col_name, operator: CompareOP, value):
            self._query += f"{col_name} {operator.value} {value}"
            self._expect_logical = True
            return self

        def equals(self, col_name, values: Tuple, reverse=False):
            values_str = self._connection._formatter(values, ", ")
            self._query += f"{col_name} {'NOT' if reverse else ''} IN ({values_str})"
            self._expect_logical = True
            return self

        def logical(self, operator: LogicalOP):
            self._query += f" {operator.value} "
            self._expect_logical = False
            return self

    class Order:
        ASCENDING = " ASC"
        DESCENDING = " DESC"

    class Clause:
        WHERE = "WHERE"
        ORDERBY = "ORDER BY"
        JOIN = "JOIN"

    class Join:
        INNER = "INNER"
        LEFT = "LEFT"
        CROSS = "CROSS"

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
        self.__query_type = type
        self.__where = False
        self.__orderby = False
        self.__join = False
        self.__data: Tuple[Any] = data
        self.__tables: List[str] = [table] if table else []
        self.__cols: List[Tuple[str]] = [col_names] if col_names else []

    def __format_null_values(self, values: Tuple[Any]):
        """Formats each of string\s values: each value in `values` set to `VALUE_NULL`
        will be inserted in the query string as 'IS_NULL' and not as '?'.
        """
        each: Tuple[str] = ()
        for value in values:
            if value == OPR.IS_NULL:
                each += (" " + OPR.IS_NULL,)
            else:
                each += (" = ?",)
        return each

    def where(self, col_names: Tuple[str, ...], values: Tuple):
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
        if self.__where:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.WHERE,
                "Can't set clause more then once",
            )
        if self.__orderby:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.WHERE,
                f"Can't set clause after '{QueryBuilder.Clause.ORDERBY}'",
            )

        each: Tuple[str] = ()
        if OPR.IS_NULL in values:
            each = self.__format_null_values(values)
            values = tuple(val for val in values if val != OPR.IS_NULL)
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
        self, col_names: Tuple[str], directions: Tuple[str] = (Order.ASCENDING,)
    ):
        """Set fields and directions for ordering documents.

        Args:
            * `col_names`: a tuple of column-names to order by.
            * `directions`(optional): a tuple of directions in order of there respective column-name.
                * possible values available in `QueryBuilder.ORDER`, default: `ORDER.ASCENDING`.

        Raises:
            * `IntegrityError` if:
                * more then one `ORDER BY` clause is set
                * `ORDER BY` clause is set in an `UPDATE` query

        Returns:
            a `QueryBuilder` instance of the orderby query.
        """
        if self.__orderby:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.ORDERBY,
                "Can't set clause more then once",
            )

        if self.__query_type == Queries.UPDATE:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.ORDERBY,
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
                * possible values available in `QueryBuilder.JOIN`, default: `None`.
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
        if self.__join:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.JOIN,
                "Can't set clause more then once",
            )
        if self.__where:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.JOIN,
                f"Can't set clause after '{QueryBuilder.Clause.WHERE}'",
            )
        if self.__orderby:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.JOIN,
                "Can't set clause after '{}'".format(QueryBuilder.Clause.ORDERBY),
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

        self.query = (
            f"SELECT {cols_query_base}, {cols_query_join} FROM {self.__tables[0]}"
        )

        src_col = kwargs.get("src_col", None)
        target_col = kwargs.get("target_col", None)

        type_str = (" " + type) if not is_empty(type) else ""
        join_data = ""

        if src_col is not None:
            target_col = src_col if target_col is None else target_col
            join_data = f" ON {self.__tables[0]}.{src_col} = {table}.{target_col}"

        self.query += type_str + f" JOIN {table}{join_data}"
        self.__tables.append(table)
        self.__join == True
        return self

    def execute(
        self,
        result_parser: Callable[[Tuple[Tuple], List[Tuple]], Any]
        | Parsers = Parsers.Tuple,
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

        if self.__query_type == Queries.UPDATE and not self.__where:
            raise QueryIntegrityError(
                self,
                self.__query_type,
                QueryBuilder.Clause.WHERE,
                "Missing clause",
            )
        return self.connection._execute(
            self.__query_type, self.query, self.__data, result_parser
        )


class QueryIntegrityError(Exception):
    def __init__(self, obj, on_query: Queries, on_clause: str, msg: str) -> None:
        super().__init__(
            f"in {obj.__class__.__name__}: query '{on_query}': {msg}: '{on_clause}"
        )
