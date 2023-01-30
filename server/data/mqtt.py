from __future__ import annotations
from typing import Dict, List, Callable, Any
import json, logging
import paho.mqtt.client as mqtt
from extensions import is_empty
from paho.mqtt.client import MQTTMessage
from datetime import datetime


class MQTTConnection(object):
    def __init__(self, name) -> None:
        self.logger = logging.getLogger(self.__class__.__name__)
        self._client: mqtt.Client = mqtt.Client(name)
        self._callbacks: Dict[str, List[Callable[[Dict[str, Any]], None]]] = {}
        self._suffixs: List[str] = [None, None]

        def on_connect(client: mqtt.Client, userdata, flags, rc):
            self.logger.info(f"{on_connect.__name__}> Connected with result code {rc}")
            topics = [(topic, 0) for topic in self._callbacks.keys()]

            if not is_empty(topics):
                client.subscribe(topics)

        def on_message(client, data, msg: MQTTMessage):
            self.__execute_callbacks(msg.topic, json.loads(msg.payload.decode()))

        self._client.on_connect = on_connect
        self._client.on_message = on_message

    def subscribe(self, topic, callback: Callable[[Dict[str, Any]], None]):
        topic_callbacks = self._callbacks.get(topic, None)
        if topic_callbacks is None:
            topic_callbacks = self._callbacks[topic] = []

        topic_callbacks.append(callback)

        self._client.subscribe(topic)

    def unsubscribe(self, topic, callback: Callable[[Dict[str, Any]], None]):
        method_sig = f"{self.__class__.__name__}.{self.unsubscribe.__name__}()"

        topic_callbacks = self._callbacks.get(topic, None)
        if topic_callbacks is not None:
            try:
                topic_callbacks.remove(callback)
            except ValueError:
                self.logger.info(
                    f"{method_sig}: callback '{callback.__name__}()' is not present under '{topic}'"
                )

            if is_empty(topic_callbacks):
                self._callbacks.pop(topic)
                self._client.unsubscribe(topic)
        else:
            self.logger.info(f"{method_sig}: topic '{topic}' is empty.")

    def publish(self, topic, data: Dict[str, Any]) -> bool:
        info = self._client.publish(
            topic, json.dumps(data, default=self.__json_serializer)
        )
        return info.is_published()

    def connect(self, broker_ip, port):
        self._client.connect(broker_ip, port)
        self._client.loop_start()

    def disconnect(self):
        self._client.loop_stop()
        self._client.disconnect()

    def __execute_callbacks(self, topic, data: Dict[str, Any]):
        topic_callbacks = self._callbacks.get(topic, None)
        if topic_callbacks is not None:
            for callback in topic_callbacks:
                callback(data)

    def __json_serializer(self, obj) -> str:
        if isinstance(obj, datetime):
            serialized = obj.isoformat()
        else:
            raise TypeError(
                f"{self.__class__.__name__}.{self.__json_serializer.__name__}(): Type {type(obj)} is not serializable"
            )

        return serialized
