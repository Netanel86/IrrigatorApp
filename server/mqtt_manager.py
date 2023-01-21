from typing import Dict, List, Callable, Any
import json
import paho.mqtt.client as mqtt
from extensions import is_empty
from infra import Loggable
from paho.mqtt.client import MQTTMessage


class MQTTManager(Loggable):
    def __init__(self, name) -> None:
        self._client: mqtt.Client = mqtt.Client(name)
        self._callbacks: Dict[str, List[Callable[[Dict[str, Any]], None]]] = {}
        self._suffixs: List[str] = [None, None]

        def on_connect(client: mqtt.Client, userdata, flags, rc):
            self.logger.info(f"Connected with result code {rc}")
            topics = [(topic, 0) for topic in self._callbacks.keys()]

            if not is_empty(topics):
                client.subscribe(topics)

        def on_message(client, data, msg: MQTTMessage):
            self.__execute_callbacks(msg.topic, json.load(msg.payload.decode()))

        self._client.on_connect = on_connect
        self._client.on_message = on_message

    def __execute_callbacks(self, topic, data: Dict[str, Any]):
        topic_callbacks = self._callbacks.get(topic, None)
        if topic_callbacks is not None:
            for callback in topic_callbacks:
                callback(data)

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

    def publish(self, topic, data: Dict[str, Any]):
        self._client.publish(topic, json.dumps(data))

    SUBSCRIBE = 0
    PUBLISH = 1

    def set_suffix(self, suffix: str, suffix_id: int):
        self._suffixs.insert(suffix_id, suffix)

    def __topic(self, topic: str, suffix_id: int) -> str:
        return (
            topic
            if self._suffixs[suffix_id] is None
            else topic + self._suffixs[suffix_id]
        )

    def connect(self, broker_ip, port):
        self._client.connect(broker_ip, port)
        self._client.loop_start()

    def disconnect(self):
        self._client.loop_stop()
        self._client.disconnect()
