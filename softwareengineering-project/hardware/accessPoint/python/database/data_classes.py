from datetime import datetime
from typing import Any
from config.configuration import ap_configuration

from log.log_service import log_setup
logger = log_setup(__name__)


class DatabaseMessage:
    """ Database Class for the message table, which is used to save messages inside the database as well as displaying saved messages.
    """    
    

    def __init__(self, timestamp: datetime, device_type: str, device_id: int, message_type: int, message: str):
        """Creation of Message object for database (messages table)

        Args:
            timestamp (datetime): Timestamp when the message was recorded.
            device_type (str): type of device (AP or TD) sending the message
            device_id (int): id of the device
            message_type (int): type of message (1 - info, 2 - warning, 3 - error)
            message (str): message itself 
        """

        self.timestamp = timestamp
        self.device_type = device_type
        self.device_id = device_id
        self.message_type = message_type
        self.message = message

    def __str__(self):
        string = f"{self.device_type} {self.device_id}: type {self.message_type} - {self.message} ({self.timestamp})"
        return string


class DatabaseMeasurement:
    """Database class for the measurement table, which is used to save measurements in the database as well as displaying saved measurements.
    """    

    def __init__(self, timestamp: datetime, tempera_id: int = None, temperature: float = None, humidity: float = None, air_quality: float = None, light_intensity: float = None):
        """Creation of Measurement object for database (measurements table)

        Args:
            timestamp (datetime): Timestamp of the measurement.
            tempera_id (int): id of tempera device the measurement is from. Defaults to None.
            temperature (float): temperature in degC. Defaults to None.
            humidity (float): humidity in %. Defaults to None.
            air_quality (float): air_quality in ??. Defaults to None.
            light_intensity (float): light intensity in Volt (want lux later?). Defaults to None.
        """

        self.tempera_id = tempera_id
        self.temperature = temperature
        self.humidity = humidity
        self.air_quality = air_quality
        self.light_intensity = light_intensity
        self.timestamp = timestamp


    def check_for_none(self):
        """Checks if any of the (not by default set parameters) are None, since the Database can't read None values.

        Returns:
            DatabaseMeasurement: Object itself when tempera_id is set with None changed to NULL
        """

        if self.tempera_id == None:
            logger.error("check_for_none: measurement without tempera_id found.")
            return None
        
        if self.air_quality == None:
            self.air_quality = "NULL"
            logger.info(f"check_for_none: air_quality was None/NULL from TD-{self.tempera_id}.")
        if self.humidity == None:
            self.humidity = "NULL"
            logger.info(f"check_for_none: humidity was None/NULL from TD-{self.tempera_id}.")
        if self.light_intensity == None:
            self.light_intensity = "NULL"
            logger.info(f"check_for_none: light_intensity was None/NULL from TD-{self.tempera_id}.")
        if self.temperature == None:
            self.temperature = "NULL"
            logger.info(f"check_for_none: temperature was None/NULL from TD-{self.tempera_id}.")
        
        return self
        
    


    def set_sensor_data(self, value, descirptor):
        """_summary_

        Args:
            value (_type_): _description_
            descirptor (_type_): _description_
        """
        value = value.decode()
        if(value == "None"):
                value = None

        if descirptor == "temperature" or descirptor == ap_configuration.uuid_temperature:
            self.temperature=  float(value)
            logger.debug(f"set_sonsor_data: temperature {self.temperature} set successful")
        elif descirptor == "humidity" or descirptor == ap_configuration.uuid_humidity:
            self.humidity=  float(value) 
            logger.debug(f"set_sonsor_data: humidity {self.humidity} set successful")
        elif descirptor == "light_intensity" or descirptor == ap_configuration.uuid_light_intensity:
            self.light_intensity=  float(value)
            logger.debug(f"set_sonsor_data: light_intensity {self.light_intensity} set successful")
        elif descirptor == "air_quality" or descirptor == ap_configuration.uuid_air_quality:
            self.air_quality=  float(value)
            logger.debug(f"set_sonsor_data: air_quality {self.air_quality} set successful")
        elif descirptor == "id" or descirptor == "Device Name" or descirptor == ap_configuration.uuid_device_name:
            idstring = value
            if(idstring[:len(ap_configuration.group)] == ap_configuration.group):
                self.tempera_id = int(idstring[len(ap_configuration.group)+4:])
                logger.info(f"set_sonsor_data: tempera_id {self.tempera_id} set successful")
            else: 
                logger.error("set_sonsor_data: wrong device")
                #self.send_status = -1
            # print("device_name ist: ", value[8:])
        else: 
            logger.error(f"set_sonsor_data: no match found for characteristic {descirptor} with value {value}")
    

    def __str__(self):
        return f"ID {self.tempera_id}: temp: {self.temperature} , hum: {self.humidity}, air: {self.air_quality}, light: {self.light_intensity} ({self.timestamp})"
        




class DatabaseTimeRecord:
    """Database class for the time_record table, which is used to save time records in the database as well as displaying saved time records.
    """    

    def __init__(self, timestamp: datetime, tempera_id: int, work_mode: str):
        """Creation of time record object for database (time_records table)

        Args:
            timestamp (datetime): timestamp when the work mode was recorded.
            tempera_id (int): id of tempera device the timerecord is from
            work_mode (str): work mode the tempera device was set to (AVAILABLE, MEETING, DEEP_WORK, OUT_OF_OFFICE)
        """        

        self.tempera_id = tempera_id
        self.work_mode = work_mode
        self.timestamp = timestamp

    def check_for_none(self):
        """Checks if any of the (not by default set parameters) are None, since the Database can't read None values.

        Returns:
            DatabaseTimeRecord: Object itself when tempera_id is set with None changed to NULL
        """

        if self.tempera_id == None:
            logger.error("check_for_none: time_record without tempera_id found.")
            return None
        
        if self.work_mode == None:
            logger.error(f"check_for_none: work_mode was None/NULL from TD-{self.tempera_id}.")
            return None
        
        return self

    def __str__(self):
        return f"ID {self.tempera_id}: work_mode: {self.work_mode} ({self.timestamp})"
    

class DababaseTemperaDevice:
    """ Database class for the tempera_device table, which is used to save tempera devices in the database as well as displaying saved tempera devices.
    """    

    def __init__(self, tempera_id: int, status: str, update_timestamp: datetime, connected: bool = False):
        """Creation of tempera device object for database (tempera_devices table)

        Args:
            tempera_id (int): id of tempera device (that is on the list allowed to be connected via BLE)
            status (str):  status of the tempera device (ENABLED, DISABLED, NOT_REGISTERED)
            update_timestamp (datetime): time of the latest update from the backend
            connected (bool): boolean that indicates if the tempera device is connected via BLE or not, defaults to False.
        """
        
        self.tempera_id = tempera_id
        self.status = status
        self.update_timestamp = update_timestamp
        self.string_id = ap_configuration.group + f"-TD-{self.tempera_id}"
        self.connected = connected

    def __str__(self):
        return self.string_id + f" status: {self.status}, connected: {self.connected} ('{self.update_timestamp}')"
    
    def __repr__(self):
        return "DababaseTemperaDevice: " + self.string_id + f" status: {self.status}, connected: {self.connected} ('{self.update_timestamp}')"
    
    def get_name_if_enabled(self):
        '''If TD status is ENABLED:
                Returns the Name in format: Group-TD-tempera_id
            otherwise it returns None
        '''
        if self.status == "ENABLED":
            return self.string_id
        else:
            return None
        
    def get_name_if_not_conneted(self):
        '''If TD connected is False:
                Returns the Name in format: Group-TD-tempera_id
            otherwise it returns None
        '''
        if not self.connected:
            return self.string_id
        else:
            return None
        
    


    