from datetime import datetime
from config.configuration import ap_configuration


class RestMeasurement:
    """Rest Class for Measurements used to send measurements to the backend.
    """    

    def __init__(self, timestamp: datetime, tempera_id: int, temperature: float, humidity: float, air_quality: float, light_intensity: float):
        """Creation of Measurment object for json to be sent via rest.

        Args:
            timestamp (datetime): timestamp when the measurement was reciorded
            tempera_id (int): id of tempera device that recorded the measurement
            temperature (float): recorded temperature
            humidity (float): recorded humidity
            air_quality (float): recorded air quality
            light_intensity (float): recorded light intensity

        id is only here because it is needed at the backend for easier mapping.
        """        

        self.id = None
        self.timestamp = timestamp.strftime(ap_configuration.time_conversion_server)
        self.temperaDeviceId = tempera_id
        self.air_temperature = temperature
        self.air_humidity = humidity
        self.air_quality = air_quality
        self.light_intensity = light_intensity


class RestTimeRecord:
    """Rest Class for Time Records used to send time records to the backend.
    """  

    def __init__(self, timestamp: datetime, tempera_id: int, work_mode: str):
        """Creation of Time_Record object for json to be sent via rest.

        Args:
            timestamp (datetime): timestamp when the work mode was recorded
            tempera_id (int): id of tempera device that recorded the work mode change
            work_mode (str): recorded work mode (AVAILABLE, MEETING, DEEP_WORK, OUT_OF_OFFICE)

        id is only here because it is needed at the backend for easier mapping.
        """

        self.id = None
        self.timestamp = timestamp.strftime(ap_configuration.time_conversion_server)
        self.temperaDeviceId = tempera_id
        self.workMode = work_mode


class RestMessage:
    """Rest Class for Messages used to send messages to the backend.
    """  

    def __init__(self, timestamp: datetime, device_type: str, device_id: int, message_type: int, message: str): # better in str form or 2 fields one with id and other with AP or TD?
        """Creation of Message object for json to be sent via rest.

        Args:
            timestamp (datetime): timestamp when the message was recorded
            device_type (str): type of device (AP or TD) the message is from
            device_id (int): id of the device
            message_type (int): type of message (1 - info, 2 - warning, 3 - error)
            message (str): recorded message

        id is only here because it is needed at the backend for easier mapping.
        """

        self.id = None
        self.timestamp = timestamp.strftime(ap_configuration.time_conversion_server)
        self.device_type = device_type
        self.device_id = device_id
        self.message_type = message_type
        self.message = message


class RestTemperaDevice:
    """Rest Class for receiving TemperaDevice updates from backend.
    """    

    def __init__(self,  timestamp: str, logStatus: str, temperaDeviceId: int, newStatus: str):
        """Creation of Tempera Device object (from json sent by the backend).

        Args:
            timestamp (str): timestamp of the update
            logStatus (str): the type of the change made to the device (CREATED, UPDATED, DELETED)
            temperaDeviceId (int): id of thempera device
            newStatus (str): new status of the device to be recorded
        """        

        self.timestamp = datetime.strptime(timestamp,ap_configuration.time_conversion_tempera_devices_server) ### change in time conversion
        self.logStatus = logStatus
        self.temperaDeviceId = temperaDeviceId
        self.newStatus = newStatus

