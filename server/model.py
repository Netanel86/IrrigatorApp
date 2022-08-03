import datetime


class Valve(object):
    PROP_ID = 'id'
    PROP_INDEX = 'index'
    PROP_DESCRIPTION = 'description'
    PROP_MAX_DURATION = 'maxDuration'
    PROP_DURATION = 'duration'
    PROP_ON_TIME = 'onTime'

    def __init__(self, index, description, max_duration):
        self.id = ""
        self.index = index
        self.description = description
        self.max_duration = max_duration
        self.duration = 0
        self.on_time = datetime.datetime.now().astimezone()

    def __str__(self):
        return "[Valve: #{0}]: {1}, Max: {2}s, Last on: {3} at {4} for {5}s".format(
            self.index, self.description, self.max_duration, 
            self.on_time.strftime('%x'),self.on_time.strftime('%X'), self.duration)

    def to_dict(self):
        return {self.PROP_ID: self.id, 
                self.PROP_INDEX: self.index, 
                self.PROP_DESCRIPTION: self.description,
                self.PROP_MAX_DURATION: self.max_duration, 
                self.PROP_DURATION: self.duration, 
                self.PROP_ON_TIME: self.on_time}

    def to_prop_dict(self, props):
        prop_dict = {}
        
        for prop in props:
            match prop:
                case self.PROP_ID: prop_dict[prop] = self.id
                case self.PROP_INDEX: prop_dict[prop] = self.index
                case self.PROP_DESCRIPTION: prop_dict[prop] = self.description
                case self.PROP_MAX_DURATION: prop_dict[prop] = self.max_duration
                case self.PROP_DURATION: prop_dict[prop] = self.duration
                case self.PROP_ON_TIME: prop_dict[prop] = self.on_time
        return prop_dict

    @staticmethod
    def from_dict(source):
        return Valve(source[Valve.PROP_ID], 
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

    def __init__(self, id, index, time, log, message = 0, duration = 0, predicate = 0):
        self.id = id
        self.index = index
        self.time = time.astimezone()
        self.log = log
        self.message = message
        self.duration = duration
        self.predicate = predicate
    
    def to_dict(self):
        return {self.PROP_ID: self.id, 
                self.PROP_INDEX: self.index, 
                self.PROP_MSG: self.message,
                self.PROP_LOG: self.log, 
                self.PROP_DURATION: self.duration, 
                self.PROP_TIME: self.time}

    @staticmethod
    def from_dict(id, source):
        return Command(id, 
                    source[Command.PROP_INDEX], 
                    source[Command.PROP_TIME], 
                    source[Command.PROP_LOG][0], 
                    source[Command.PROP_MSG], 
                    source[Command.PROP_DURATION])

    def __str__(self):
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
        


