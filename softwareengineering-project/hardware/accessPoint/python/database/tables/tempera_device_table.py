from datetime import datetime
from database.data_classes import DababaseTemperaDevice
from config.configuration import ap_configuration

from log.log_service import log_setup
logger = log_setup(__name__)


class TemperaDeviceTable:
    """
    A class used for operations on the tempera_device table in a given sqlite3 database.

    Attributes
    ----------
    connection
        connection to databese
    curser
        curser corresponding to the connection
    arduino_id_beginning
        string that stands in front of each arduino id for BLE connections

    Methods
    -------
    save(tempera_id, status, update_time)
        saves the given tempera device in tempera_devices table.
    get(tempera_id=None, status=None)
        gets the tempera devices of a specified tempera device or all devices with a specified status (or all if None) from the database.
    print(tempera_id=None, status=None)
        prints all the tempera_devices of a specific tempera device/status (or all if None) that are currently saved in the database
    delete(tempera_id)
        deletes the entry of a specific tempera device by tempera_id
    clear()
        CLears the tempera device table (all data)
    device_connected(tempera_id)
        Sets the Tempera Device with given tempera_id connected (BLE)
    device_disconnected(tempera_id)
        Sets the Tempera Device with given tempera_id to disconnected (BLE)
    diconnect_all()
        Sets all Tempera Devices to disconnected (BLE)
    """

    def __init__(self, connection, curser):
        """sets up the table of tempera devices the AP knows in the given database

        Args:
            connection (Any): connection to databese
            curser (Any): curser corresponding to the connection

        tempera_device table details:
            tempera_id (int): id of the tempera device
            status (str): status of the tempera device (ENABLED, DISABLED, 2 - NOT_REGISTERED)
            string_id (str): tempera device id as string (with company abbreviation for BLE)
            update_timestamp (datetime): time of the latest update from the backend
            connected (bool): if tempera device is connected via BLE or not
        """        

        self.connection = connection
        self.curser = curser
        self.arduino_id_beginning = ap_configuration.group + "-A-"

        self.curser.execute("CREATE TABLE IF NOT EXISTS tempera_devices (tempera_id INTEGER, status TEXT, update_timestamp TEXT, string_id TEXT, connected INTEGER)")

    
    def save(self, tempera_device: DababaseTemperaDevice):
        """saves the given tempera device in tempera_devices table.

        If the id already exists then the existing data will be updated if the timestamp of the given update is newer than the saved update_timestamp.

        Args:
            tempera_device (DatabaseTemperaDevice): tempera device object to be saved in the database
        """        
        saved_arduino = self.get(tempera_id=tempera_device.tempera_id)

        if tempera_device.connected:
            bool_connnected = 1
        else:
            bool_connnected = 0

        try:
            # check if the id already exists in db
            if saved_arduino == []:
                self.curser.execute(f"""
                    INSERT INTO tempera_devices 
                    VALUES  ({tempera_device.tempera_id}, '{tempera_device.status}', '{tempera_device.update_timestamp}', '{tempera_device.string_id}', {bool_connnected})
                """) 

            else:
                saved_update_time = saved_arduino[0].update_timestamp
                # check if update is newer as the saved one
                if tempera_device.update_timestamp > saved_update_time:
                    self.curser.execute(f"UPDATE tempera_devices SET status = '{tempera_device.status}' WHERE tempera_id = {tempera_device.tempera_id}") 
                    self.curser.execute(f"UPDATE tempera_devices SET update_timestamp = '{tempera_device.update_timestamp}' WHERE tempera_id = {tempera_device.tempera_id}")
                else:
                    logger.info("save: old update will not be saved.")

            self.connection.commit()
        except AttributeError:
            logger.error("save: given object is not DatabaseTemperaDevice! Please Insert a DatabaseTemperaDevice object.")



    def get(self, tempera_id: int =None, status: int =None):
        """gets the tempera devices of a specified tempera_id or all devices with a specified status (or all if None) from the database.

        Args:
            tempera_id (int, optional): id of tempera device that shall be found. Defaults to None (all devices).
            status (str, optional): status of the tempera device (ENABLED, DISABLED, NOT_REGISTERED)
        Returns:
            [DatabaseTemperaDevice]: list of tempera devices
        """        	

        if tempera_id == None:
            if status == None:
                statement = "SELECT * FROM tempera_devices"
            else:
                statement = f"SELECT * FROM tempera_devices WHERE status = '{status}'"
        else:
            #check if is really id ?
            if status == None:
                statement = f"SELECT * FROM tempera_devices WHERE tempera_id = {tempera_id}"
            else:
                statement = f"SELECT * FROM tempera_devices WHERE tempera_id = {tempera_id} and status = '{status}'"

        received_data = self.curser.execute(statement).fetchall()

        if received_data == []:
            logger.info("Empty get statement.")

        list_tempera_devices =[]
        for data in received_data:
            list_tempera_devices.append(DababaseTemperaDevice(data[0],data[1],datetime.strptime(data[2],ap_configuration.time_conversion_ap),data[4]==1)) ### changed time conversion

        return list_tempera_devices
    

    def print(self, tempera_id: int=None, status: int =None):
        """prints all the tempera devices of a specific id/status (or all if None) that are currently saved in the database

        Args:
            tempera_id (int, optional): id of tempera device that shall be printed. Defaults to None (all devices).
            status (str, optional): status of the tempera devices that shall be printed. (ENABLED, DISABLED, NOT_REGISTERED)
        """
        # check if real id or None?
        arduinos = self.get(tempera_id=tempera_id, status=status)

        if tempera_id == None:
            if status == None:
                print("\nAll tempera devices:\n-------------------")
            else:
                print(f"\nAll tempera devices with status '{status}':\n-------------------------------")
        else:
            if status == None:
                print(f"\nTempera Ddvice with ID {tempera_id}:\n-----------------------------------")
            else:
                print(f"\nTempera device with ID {tempera_id} and status '{status}':\n-------------------------------------------")
        
        for a in arduinos:
            print(a)


    def delete(self, tempera_id: int):
        """deletes the entry of a specific tempera device by tempera_id

        Args:
            tempera_id (int): id of tempera device that shall be deleted from db
        """        

        self.curser.execute(f"DELETE FROM tempera_devices WHERE tempera_id = {tempera_id}")
        self.connection.commit()

        logger.info("Element(s) deleted")


    def clear(self):
        """CLears the tempera device table (all data)
        """

        self.curser.execute("DELETE FROM tempera_devices")
        self.connection.commit()
        logger.info("tempera device table cleared")


    def device_connected(self,tempera_id: int):
        """Sets the Tempera Device with given tempera_id connected (BLE)

        Args:
            tempera_id (int): ID of Tempera Device that shall be set to connected
        """        

        self.curser.execute(f"UPDATE tempera_devices SET connected = 1 WHERE tempera_id = {tempera_id}")
        self.connection.commit()
        logger.info(f"TD {tempera_id} connected")


    def device_disconnected(self,tempera_id: int):
        """Sets the Tempera Device with given tempera_id to disconnected (BLE)

        Args:
            tempera_id (int): ID of Tempera devie that shall be set to disconnected
        """        

        self.curser.execute(f"UPDATE tempera_devices SET connected = 0 WHERE tempera_id = {tempera_id}")
        self.connection.commit()
        logger.info(f"TD {tempera_id} disconnected")


    def diconnect_all(self):
        """Sets all Tempera Devices to disconnected (BLE)
        """

        self.curser.execute(f"UPDATE tempera_devices SET connected = 0")
        self.connection.commit()
        logger.info("all TD disconnected")