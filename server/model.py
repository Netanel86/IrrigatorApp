import datetime
from typing import Any, Dict, List


class Valve(object):
    PROP_ID = 'id'
    PROP_INDEX = 'index'
    PROP_DESCRIPTION = 'description'
    PROP_MAX_DURATION = 'maxDuration'
    PROP_DURATION = 'duration'
    PROP_ON_TIME = 'onTime'

    def __init__(self, index: int, description: str, max_duration: int) -> None:
        self.id = ""
        self.index = index
        self.description = description
        self.max_duration = max_duration
        self.duration = 0
        self.on_time = datetime.datetime.now().astimezone()

    def __str__(self) -> str:
        return "[Valve: #{0}]: {1}, Max: {2}s, Last on: {3} at {4} for {5}s".format(
            self.index, self.description, self.max_duration, 
            self.on_time.strftime('%x'),self.on_time.strftime('%X'), self.duration)

    def to_dict(self) -> Dict[str, Any]:
        """Parses :class:`Valve` to a dictionary.\n
        Returns:
            A dictionary with this :class:`Valve` propeties, name and value pairs"""
        return {self.PROP_INDEX: self.index, 
                self.PROP_DESCRIPTION: self.description,
                self.PROP_MAX_DURATION: self.max_duration, 
                self.PROP_DURATION: self.duration, 
                self.PROP_ON_TIME: self.on_time}

    def to_prop_dict(self, props: List[str]) -> Dict[str, Any]:
        """Parses only the specified :class:`Valve` properties to a dictionary.\n
        Args:
            props -- a list of properties names to parse
        Returns:
            A dictionary with the specified propeties, name and value pairs
        """
        prop_dict = {}
        
        for prop in props:
            match prop:
                case self.PROP_INDEX: prop_dict[prop] = self.index
                case self.PROP_DESCRIPTION: prop_dict[prop] = self.description
                case self.PROP_MAX_DURATION: prop_dict[prop] = self.max_duration
                case self.PROP_DURATION: prop_dict[prop] = self.duration
                case self.PROP_ON_TIME: prop_dict[prop] = self.on_time
        return prop_dict

    @staticmethod
    def from_dict(valve_id: str, source: Dict[str, Any]):
        """Parses and creates a new :class:`Valve` from a dictionary.\n
        Args:
            valve_id -- the valve's database id\n
            source -- a dictionary with object's properties, name and value pairs\n
        Returns:
            :class:`Valve` -- a Valve initialized with dictionary data\n
        Remarks:
            use the :class:`Valve` constant property name fields: 
                :field:`Valve.PROP_INDEX`\n
                :field:`Valve.PROP_DESCRIPTION`\n
                :field:`Valve.PROP_MAX_DURATION`\n
                :field:`Valve.PROP_DURATION`\n
                :field:`Valve.PROP_ON_TIME`
        """
        return Valve(valve_id, 
                    source[Valve.PROP_INDEX], 
                    source[Valve.PROP_DESCRIPTION], 
                    source[Valve.PROP_MAX_DURATION], 
                    source[Valve.PROP_DURATION], 
                    source[Valve.PROP_ON_TIME])



class Command(object):
    PROP_ID = 'id'
    PROP_INDEX = 'index'
    PROP_MSG = 'description'
    PROP_LOG = 'commandLog'
    PROP_DURATION = 'duration'
    PROP_TIME = 'time'

    def __init__(self, id: str, index: int, time: datetime, log: str, message: str = 0, duration: int = 0, predicate: Any = 0) -> None:
        self.id = id
        self.index = index
        self.time = time.astimezone()
        self.log = log
        self.message = message
        self.duration = duration
        self.predicate = predicate
    
    def to_dict(self) -> dict[str, Any]:
        """Parses :class:`Command` to a dictionary.\n
        Returns:
            A dictionary with this :class:`Command` propeties, name and value pairs"""
        return {self.PROP_INDEX: self.index, 
                self.PROP_MSG: self.message,
                self.PROP_LOG: self.log, 
                self.PROP_DURATION: self.duration, 
                self.PROP_TIME: self.time}

    @staticmethod
    def from_dict(cmnd_id: str, source: Dict[str, Any]):
        """Parses and creates a new :class:`Command` from a dictionary.\n
        Args:
            cmnd_id -- the command's database id\n
            source -- a dictionary with object's properties, name and value pairs\n
        Returns:
            :class:`Command` -- a Command initialized with dictionary data\n
        Remarks:
            use the :class:`Command` constant property name fields: 
                :field:`Command.PROP_INDEX`\n
                :field:`Command.PROP_MSG`\n
                :field:`Command.PROP_LOG`\n
                :field:`Command.PROP_DURATION`\n
                :field:`Command.PROP_TIME`
        """
        return Command(cmnd_id, 
                    source[Command.PROP_INDEX], 
                    source[Command.PROP_TIME], 
                    source[Command.PROP_LOG][0], 
                    source[Command.PROP_MSG], 
                    source[Command.PROP_DURATION])

    def __str__(self) -> str:
        to_string = str.format("[{0}: Command]:", self.time.strftime('%Y-%m-%d %X'))

        match self.log:
            case 'on': to_string = "{0} Turn ON Valve #{1} for: {2}s".format(
                to_string, self.index, self.duration)
            case 'off': to_string = "{0} Turn OFF Valve #{1}".format(
                to_string, self.index)
            case 'description': to_string = "{0} Edit Valve #{1} description to: {2}".format(
                to_string, self.index, self.message)

        return to_string

    class Predicate(object):
        def __init__(self, type, value, condition = 0, duration = 0):
            self.type = type
            self.value = value
            self.condition = condition
            self.duration = duration
        


