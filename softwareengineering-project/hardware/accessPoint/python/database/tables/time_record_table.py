from database.data_classes import DatabaseTimeRecord
from datetime import datetime
from config.configuration import ap_configuration

from log.log_service import log_setup
logger = log_setup(__name__)


class TimeRecordTable:
    """
    A class used for operations on the time_records table in a given sqlite3 database.

    Attributes
    ----------
    connection
        connection to databese
    curser
        curser corresponding to the connection

    Methods
    -------
    save(time_record)
        saves the given time_record in time_records
    get(tempera_id=None)
        gets all the time records of a specified tempera device (or all if None) from the database
    print(tempera_id=None)
        prints all the time records of a specific tempera device(or all if None) that are currently saved in the database
    delete_element(time_record)
        deletes a single element from the database
    clear()
        Clears the message table (all data)
    """


    def __init__(self, connection, curser):
        """sets up the time record table in the given database

        Args:
            connection (Any): connection to databese
            curser (Any): curser corresponding to the connection

        time record table details:
            tempera_id (int): id of the tempera device the measurement is from
            work_mode (str): work mode the device was set to (AVAILABLE, MEETING, DEEP_WORK, OUT_OF_OFFICE)
            timestammp (datetime): timestamp when the work mode was recorded
        """        

        self.connection = connection
        self.curser = curser

        self.curser.execute("CREATE TABLE IF NOT EXISTS time_records (tempera_id INTEGER, work_mode TEXT, timestamp datetime)")


    def save(self, time_record: DatabaseTimeRecord):
        """saves the given time_record in time_records

        Args:
            time_record (DatabaseTimeRecord): DatabaseTimeRecord object to be saved in the database
        """        
        try:
            time_record = time_record.check_for_none()
            if time_record == None: #no save if tempera_id is not set
                return

            self.curser.execute(f"""
                INSERT INTO time_records 
                VALUES  ({time_record.tempera_id}, '{time_record.work_mode}', '{time_record.timestamp}')
            """)

            self.connection.commit()

        except AttributeError:
            logger.error("save: given object is not DatabaseTimeRecord! Please Insert a DatabaseTimeRecord object.")


        


    def get(self, tempera_id: int =None):
        """gets all the time records of a specified tempera device/send_status (or all if None) from the database

        Args:
            tempera_id (int, optional): id of tempera device the time records shall be taken from. Defaults to None (all devices).

        Returns:
            [DatabaseTimeRecord]: list of time records 
        """        	

        if tempera_id == None:
            statement = "SELECT * FROM time_records"
        else:
            statement = f"SELECT * FROM time_records WHERE tempera_id = {tempera_id}"

        received_data = self.curser.execute(statement).fetchall()

        if received_data == []:
            logger.info("Empty get statement.")

        list_time_records = []
        for data in received_data:
            list_time_records.append(DatabaseTimeRecord(datetime.strptime(data[2],ap_configuration.time_conversion_ap),data[0],data[1]))
        
        return list_time_records
    

    def print(self, tempera_id: int=None):
        """prints all the time records of a specific tempera device/send_status (or all if None) that are currently saved in the database

        Args:
            tempera_id (int, optional): id of tempera device whose time records shall be printed. Defaults to None (all devices).
        """
        measurements = self.get(tempera_id=tempera_id)

        if tempera_id == None:
            print("\nAll time records:\n-------------------")
        else:
            print(f"\nAll time records from ID {tempera_id}:\n-----------------------------------")
            
        for m in measurements:
            print(m)


    def delete_element(self, time_record: DatabaseTimeRecord):
        """deletes a single element from the database.

        Args:
            time_record (DatabaseTimeRecord): time reord to be deleted from datatbase.
        """  

        self.curser.execute(f"DELETE FROM time_records WHERE tempera_id = {time_record.tempera_id} and timestamp = '{time_record.timestamp}'")
        self.connection.commit()

        logger.info("Element(s) deleted")

    
    def clear(self):
        """Clears the time record table (all data)
        """

        self.curser.execute("DELETE FROM time_records")
        self.connection.commit()
        logger.info("time record table cleared")

    