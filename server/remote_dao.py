
from typing import Tuple
from extensions import get_cls_fields_values, reverse_dict
from model import *

#region Constants
class ModulesConsts:
    class FieldName:
        IP              ="ip",         
        DESCRIPTION     ="description",
        MAX_DURATION    ="maxDuration",
        DURATION        ="duration",   
        ON_TIME         ="onTime"      
    
    COLLECTION_NAME : str = "Modules"
    """'Modules' collection name"""
    
    FIELDS : Tuple[str,...] = get_cls_fields_values(FieldName)
    """A tuple of 'Modules' collection fields"""
    
    MAP_FROM_REMOTE: Dict[str,str] = {
        FieldName.IP          :EPModule.Props().MAC_ID      ,   
        FieldName.DESCRIPTION :EPModule.Props().DESCRIPTION ,   
        FieldName.MAX_DURATION:EPModule.Props().MAX_DURATION,       
        FieldName.DURATION    :EPModule.Props().DURATION    ,   
        FieldName.ON_TIME     :EPModule.Props().ON_TIME        
    }
    """A Mapping of remote database 'Modules' collection fields to `ModelLib.EPModule` properties (key=field_name, val=prop_name)."""

    MAP_TO_REMOTE: Dict[str,str] = reverse_dict(MAP_FROM_REMOTE)
    """A Mapping of `ModelLib.EPModule` properties to remote database 'Modules' collection fields (key=prop_name, val=field_name)."""

class SensorsConsts:
    class FieldName:
        TYPE    ="type",  
        MIN_VAL ="minVal",
        MAX_VAL ="maxVal",
        CURR_VAL="currVal"
    
    COLLECTION_NAME : str ="Sensors"
    
    FIELDS : Tuple[str,...] = get_cls_fields_values(FieldName)
    """A tuple of 'Sensors' collection fields"""
    
    MAP_FROM_REMOTE: Dict[str,str] = {
    FieldName.TYPE    :AnalogSensor.Props().TYPE       ,
    FieldName.MIN_VAL :AnalogSensor.Props().MIN_VALUE  ,
    FieldName.MAX_VAL :AnalogSensor.Props().MAX_VALUE  ,
    FieldName.CURR_VAL:AnalogSensor.Props().CURRENT_VAL    
    }
    """A Mapping of remote database 'Sensors' collection fields to `model.Sensor` properties (key=field_name, val=prop_name)."""
   
    MAP_TO_REMOTE: Dict[str,str] = reverse_dict(MAP_FROM_REMOTE)
    """A Mapping of `model.Sensor` properties to remote database 'Sensors' collection fields (key=prop_name, val=field_name)."""

class CommandsConsts:
    class FieldName:
        ACTION  ="action",   
        TIME    ="timestamp",
        ATTR    ="attributes"
    
    COLLECTION_NAME : str ="Commands"

    MAP_FROM_REMOTE = {
        FieldName.ACTION: Command.Props().ACTION,
        FieldName.TIME: Command.Props().TIME,
        FieldName.ATTR: Command.Props().ATTR,
    }
    """A Mapping of remote database 'Commands' collection fields to `model.Command` properties (key=field_name, val=prop_name)."""
    
class Constants:
    Modules = ModulesConsts
    Sensors = SensorsConsts
    Commands = CommandsConsts
#endregion





