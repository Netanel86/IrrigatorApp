from __future__ import annotations
from enum import Enum
import os
import sqlite3
from sqlite3 import Cursor, Error, IntegrityError
from pathlib import Path
from typing import Any, Dict, List, Tuple, Type

TYPE_TEXT = "TEXT"
TYPE_INT = "INTEGER"
TYPE_TIME = "TIMESTAMP"
TYPE_FLOAT = "FLOAT"


class SQLiteConnection(object):
    def __init__(self) -> None:
        self.__PATH = os.path.join(
            os.path.dirname(os.path.abspath(__file__)), "sqlite\db\pysqlite.db"
        )
        self.query_cache: Dict[str, str] = {}
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
        path_tupel = os.path.split(self.__PATH)

        Path(path_tupel[0]).mkdir(parents=True, exist_ok=True)

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
        cols_query = ""
        for idx, col in enumerate(data):
            name = col[0]
            type = col[1]
            if name == "id" and idx != 0:
                raise ValueError(
                    "{}.{}(): 'id' should be the first (index = 0) name, type pair in col_data, current 'id' index is {}".format(
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

    def insert(
        self, table: str, col_names: Tuple[str], values: Tuple[Any] | List[Tuple[Any]]
    ) -> str | int:
        cache_key = Queries.INSERT.value + " {}".format(table)
        query = self.query_cache.get(cache_key, None)

        if query is None:
            cols_query = self._formatter(col_names, ",")
            vals_query = self._formatter("?", ",", len(col_names))
            query = "INSERT INTO {}({}) VALUES ({})".format(
                table, cols_query, vals_query
            )
            self.query_cache[cache_key] = query

        if not isinstance(values, list):
            query += " RETURNING id"

        return self._execute(Queries.INSERT, query, values)

    def update(
        self, table: str, col_names: Tuple[str], values: Tuple[Any]
    ) -> QueryBuilder:
        cols_query = self._formatter(
            col_names,
            ",",
            each=" = ?",
        )
        query = "UPDATE {} SET {}".format(table, cols_query)
        return QueryBuilder(self, Queries.UPDATE, query, values)

    def delete(self, table: str, id: str | int = None) -> bool:
        if id is None:
            query = "DROP TABLE IF EXISTS {}".format(table)
        else:
            query = "DELETE FROM {} WHERE id = ?".format(table)
        return self._execute(Queries.DELETE, query, id)

    def select(self, table: str, col_names: Tuple[str] = None) -> QueryBuilder:
        query = None
        if col_names is None:
            cache_key = Queries.SELECT.value + " {}".format(table)
            query = self.query_cache.get(cache_key, None)

        if query is None:
            cols_query = (
                self._formatter(col_names, ", ") if col_names is not None else "*"
            )
            query = "SELECT {} FROM {}".format(cols_query, table)
            if col_names is None:
                self.query_cache[cache_key] = query

        return QueryBuilder(self, Queries.SELECT, query)

    def map_to_object(
        self, tuples: List[Tuple[Tuple]], object_type: Type[Any], key_idx: int = None
    ) -> Dict[str, Any] | List:
        """Map a list of tuples to a dict of `object_type` values

        Args:
            tuples: a list of tuples to map.
            object_type: the target type for mapped objects.
            key_idx(optional): the property index to be used as key in the returned dict.
                default: None.

        Returns:
            A collection of `object_type`: If `key_idx` is set returns a dict, otherwise returns a list.

        Raises:
            AttributeError: if the type `object_type` does not implement static method `from_tuple(source)`

            IndexError: if `key_idx` is out of `tuples` range
        """
        IDX_VAL = 1
        objects: Dict[str, Any] | List = {} if key_idx is not None else []

        for tup in tuples:
            mapped_obj = object_type.from_tuple(tup)
            if key_idx is not None:
                objects[tup[key_idx][IDX_VAL]] = mapped_obj
            else:
                objects.append(mapped_obj)

        return objects

    def _execute(
        self,
        q_type: Queries,
        query: str,
        values: Tuple[Any] | List[Tuple] = None,
    ):
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

                case Queries.SELECT:
                    result = self.__format_result(cursor.fetchall(), cursor.description)

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

    def __format_result(self, values: List[Tuple], col_data: Tuple[Tuple]):
        result: List[Tuple[Tuple]] | Tuple[Tuple] = []
        for tup_obj in values:
            final_tup = self.__merge_col_val(col_data, tup_obj)
            if len(result) == 1:
                result = final_tup
            else:
                result.append(final_tup)
        return result

    def __merge_col_val(self, col_names: Tuple, values: Tuple) -> Tuple[Tuple]:
        """Merge column names and values to a single tuple of tuple pairs, where each
        column name is matched to a value to create a tuple pair.

        e.g:
            new_tuple = (
                (col_names[0], values[0]),
                (col_names[1], values[1]),
                ...
            )
        Args:
            col_names: the column names in order of there respective value
            values: the values in order of there respective column name

        Returns:
            A tuple containing column name and value tuple pairs
        """
        final_tup = ()
        for col, val in col_names, values:
            final_tup += ((col[0], val),)
        return final_tup

    def _formatter(self, data: Any, separator: str, count: int = None, **kwargs) -> str:
        each: str | Tuple[Any] = kwargs.get("each", "")
        last: str = kwargs.get("last", "")
        query = ""
        if isinstance(data, tuple):
            for idx, string in enumerate(data):
                query += string + (each if isinstance(each, str) else each[idx])
                query += separator if idx < len(data) - 1 else ""
        else:
            for idx in range(count):
                query += data
                query += separator if idx < count - 1 else ""

        query += last

        return query

    def close(self):
        if self.__db is not None:
            self.__db.close()


class QueryBuilder(object):
    ORDER_ASC = " ASC"
    ORDER_DESC = " DESC"

    def __init__(
        self,
        connection: SQLiteConnection,
        type: Queries,
        base_query: str,
        data: Tuple[Any] = None,
    ) -> None:
        self.query = base_query
        self.connection = connection
        self.__type = type
        self.__where = False
        self.__orderby = False
        self.__data: Tuple[Any] = data

    def __assert(self, method: str, value: bool, error_msg: str):
        if value:
            raise IntegrityError(
                "{}.{}(): {}.".format(self.__class__.__name__, method, error_msg)
            )

    def where(self, col_names: Tuple[str], values: Tuple[Any] = None):
        self.__assert(
            self.where.__name__,
            self.__where == True,
            "only a single 'WHERE' clause is allowed",
        )
        self.__assert(
            self.where.__name__,
            self.__orderby == True,
            "'WHERE' clause should be declared before the 'ORDER BY' clause.",
        )

        cols_query = self.connection._formatter(col_names, " AND ", each=" = ?")

        self.query += " WHERE {}".format(cols_query)

        if self.__data == None:
            self.__data = values
        else:
            self.__data += values

        self.__where = True
        return self

    def orderby(self, col_names: Tuple[str], directions: Tuple[str] = (ORDER_ASC,)):
        self.__assert(
            self.orderby.__name__,
            self.__orderby == True,
            "only a single 'ORDER BY' clause is allowed.",
        )
        self.__assert(
            self.orderby.__name__,
            self.__type == Queries.UPDATE,
            "no 'ORDER BY' clause is allowed in 'UPDATE' query",
        )

        cols_query = self.connection._formatter(col_names, ", ", each=directions)
        self.query += " ORDER BY {}".format(cols_query)
        self.__orderby = True
        return self

    def execute(self):
        return self.connection._execute(self.__type, self.query, self.__data)


class Queries(Enum):
    CREATE = "create"
    INSERT = "insert"
    UPDATE = "update"
    SELECT = "select"
    DELETE = "delete"


# ret_val = (
#     con.update(
#         "modules",
#         ("duration", "on_time", "description"),
#         (200, datetime.now().astimezone(), "hello"),
#     )
#     .where(("id",), (module.id,))
#     .execute()
# )
# print(ret_val)
# ret_val = self.delete("modules")
# ret_val = con.select(
#     "modules",
#     (
#         "id",
#         "ip",
#         "description",
#         "max_duration",
#         "duration",
#         "on_time",
#         "port",
#         "timeout",
#     ),
# )
# ret_val = con.select("modules").orderby(("ip",), _QueryBuilder.ORDER_ASC).execute()
# ret_val = (
#     con.select("modules")
#     .where(("ip",), ("192.168.0.208",))
#     .orderby(("on_time",), QueryBuilder.ORDER_ASC)
#     .execute()
# )
# ret_val = con.is_exists("systems")
# print(ret_val)

# con.close()
