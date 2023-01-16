from typing import Dict, List, Callable, Any
import logging
import json
import paho.mqtt.client as mqtt
from paho.mqtt.client import MQTTMessage
from PyExtensions import isEmpty

class MQTTManager:
    def __init__(self, name) -> None:
        self.__client: mqtt.Client = mqtt.Client(name)
        self.__callbacks: Dict[str, List[Callable[[Dict[str, Any]], None]]] = {}

        def on_connect(client: mqtt.Client, userdata, flags, rc):
            logging.info(f"Connected with result code {rc}")
            topics = [(topic, 0) for topic in self.__callbacks.keys()]

            if not isEmpty(topics):
                client.subscribe(topics)

        def on_message(client, data, msg: MQTTMessage):
            self.__execute_callbacks(msg.topic, json.load(msg.payload.decode()))

        self.__client.on_connect = on_connect
        self.__client.on_message = on_message

    def __execute_callbacks(self, topic, data: Dict[str, Any]):
        topic_callbacks = self.__callbacks.get(topic, None)
        if topic_callbacks is not None:
            for callback in topic_callbacks:
                callback(data)

    def subscribe(self, topic, callback: Callable[[Dict[str, Any]], None]):
        topic_callbacks = self.__callbacks.get(topic, None)
        if topic_callbacks is None:
            topic_callbacks = self.__callbacks[topic] = []

        topic_callbacks.append(callback)

        self.__client.subscribe(topic)

    def unsubscribe(self, topic, callback: Callable[[Dict[str, Any]], None]):
        method_sig = f"{self.__class__.__name__}.{self.unsubscribe.__name__}()"

        topic_callbacks = self.__callbacks.get(topic, None)
        if topic_callbacks is not None:
            try:
                topic_callbacks.remove(callback)
            except ValueError:
                logging.info(
                    f"{method_sig}: callback '{callback.__name__}()' is not present under '{topic}'"
                )

            if isEmpty(topic_callbacks):
                self.__callbacks.pop(topic)
                self.__client.unsubscribe(topic)
        else:
            logging.info(f"{method_sig}: topic '{topic}' is empty.")

    def publish(self, topic, data: Dict[str, Any]):
        self.__client.publish(topic, json.dumps(data))

    def connect(self, broker_ip, port):
        self.__client.connect(broker_ip, port)
        self.__client.loop_start()

    def disconnect(self):
        self.__client.loop_stop()
        self.__client.disconnect()

