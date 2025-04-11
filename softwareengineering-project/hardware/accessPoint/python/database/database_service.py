import sqlite3
import database.tables.message_table as message
import database.tables.measurement_table as measurement
import database.tables.time_record_table as time_record
import database.tables.tempera_device_table as tempera_device

class DatabaseService:
    """
    A class used for operations on the database sqlite3.

    Attributes
    ----------
    connection
        connection to databese
    curser
        curser corresponding to the connection
    messages
        message table of this database  
    measurements
        measurement table of this database
    time_records
        time record table of this database
    tempera_devices
        tempera_device table of this database containing the list of accessable tempera devices
    """

    def __init__(self): # should id be hard coded inside init instead?
        """Setup of DatabaseService.

        It connects the sqlite3 databse and creates the necessary table objects (messages and measurements) if they do not already exist in the database.
        
       message table details:
            device_type (str): type of device (AP or TD) the message is from
            device_id (int): id of device
            message_type (int): type of message (1 - info, 2 - warning, 3 - error)
            message (str): message recorded
            timestamp (datetime): timestamp of message recording
        
        measurement table details:
            tempera_id (int): id of the tempera station the measurement is from
            temperature (float): measured temperature in deg C
            humidity (float): measured humidity in %
            air_quality (float): measured air_quality in ??
            light_intensity (float): measured light_intensity in Volt (want lux later?)
            timestammp (datetime): timestamp when the measurement was recorded
        
        time record table details:
            tempera_id (int): id of the tempera station the measurement is from
            work_mode (str): work mode the station was set to (AVAILABLE, MEETING, DEEP_WORK, OUT_OF_OFFICE)
            timestammp (datetime): timestamp when the measurement was recorded

        tempera_device table details:
            tempera_id (int): id of the tempera station/arduino
            status (str): status of the arduino (ENABLED, DISABLED, NOT_REGISTERED)
            string_id (str): arduino id as string (with company abbreviation for BLE)
            update_timestamp (datetime): time of the latest update from the backend
            connected (bool): if tempera device is connected via BLE or not
        """        

        # connection itself
        self.connection = sqlite3.connect('accesspoint.db')
        self.curser = self.connection.cursor()

        # create necessary Tables
        self.messages = message.MessageTable(self.connection, self.curser)
        self.measurements = measurement.MeasurementTable(self.connection, self.curser)
        self.time_records = time_record.TimeRecordTable(self.connection, self.curser)
        self.tempera_devices = tempera_device.TemperaDeviceTable(self.connection, self.curser)
        


    