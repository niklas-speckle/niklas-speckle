from database.data_classes import DatabaseMeasurement
from datetime import datetime
from config.configuration import ap_configuration

from log.log_service import log_setup
logger = log_setup(__name__)


class MeasurementTable:
    """
    A class used for operations on the measurements table in a given sqlite3 database.

    Attributes
    ----------
    connection
        connection to databese
    curser
        curser corresponding to the connection

    Methods
    -------
    save(measurement)
        saves the given measurement in measurements
    get(tempera_id=None)
        gets all the measurements of a specified tempera device(or all if None) from the database
    print(tempera_id=None)
        prints all the measurements of a specivic tempera device (or all if None) that are currently saved in the database
    delete_element(measurement)
        deletes a single element from the database
    clear()
        Clears the message table (all data)
    """

    def __init__(self, connection, curser):
        """sets up the measurement table in the given database

        Args:
            connection (Any): connection to databese
            curser (Any): curser corresponding to the connection

        measurement table details:
            tempera_id (int): id of the tempera station the measurement is from
            temperature (float): measured temperature in deg C
            humidity (float): measured humidity in %
            air_quality (float): measured air_quality in ??
            light_intensity (float): measured light_intensity in Volt (want lux later?)
            timestammp (datetime): timestamp when the measurement was recorded
        """        

        self.connection = connection
        self.curser = curser

        self.curser.execute("CREATE TABLE IF NOT EXISTS measurements (tempera_id INTEGER, temperature REAL, humidity REAL, air_quality REAL, light_intensity REAL, timestamp datetime)")


    
    def save(self, measurement: DatabaseMeasurement):
        """saves the given measurement in measurements

        Args:
            measurement (DatabaseMeasurement): DatabaseMeasurement object containing tempera_id, temperature, humidity, air_quality and light_intensity
        """        

        try:
            measurement = measurement.check_for_none()
            if measurement == None: #no save if tempera_id is not set
                return

            self.curser.execute(f"""
                INSERT INTO measurements 
                VALUES  ({measurement.tempera_id}, {measurement.temperature}, {measurement.humidity}, {measurement.air_quality}, {measurement.light_intensity}, '{measurement.timestamp}')
            """) 

            self.connection.commit()
            
        except AttributeError:
            logger.error("save: given object is not DatabaseMeasurement! Please Insert a DatabaseMeasurement object.")



    def get(self, tempera_id: int =None):
        """gets all the measurements of a specified tempera device (or all if None) from the database

        Args:
            tempera_id (int, optional): id of tempera device the measurements shall be taken from. Defaults to None (all devices).

        Returns:
            [DatabaseMeasurement]: List of DatabaseMeasurement objects containing all the information saved in the database.
        """        	

        if tempera_id == None:
            statement = "SELECT * FROM measurements"
        else:
            statement = f"SELECT * FROM measurements WHERE tempera_id = {tempera_id}"


        found_data = self.curser.execute(statement).fetchall()

        if found_data == []:
            logger.info("Empty get statement.")

        list_measurements = []
        for data in found_data:
            list_measurements.append(DatabaseMeasurement(datetime.strptime(data[5], ap_configuration.time_conversion_ap),data[0],data[1],data[2],data[3],data[4]))

        return list_measurements
    

    def print(self, tempera_id: int=None):
        """prints all the measurements of a specific tempera device (or all if None) that are currently saved in the database

        Args:
            tempera_id (int, optional): id of tempera device whose measurements shall be printed. Defaults to None (all devices).
        """
        # check if real id or None?
        measurements = self.get(tempera_id=tempera_id)

        if tempera_id == None:
            print("\nAll measurements:\n-------------------")
        else:
            print(f"\nAll measurements from ID {tempera_id}:\n-----------------------------------")
        
        for m in measurements:
            print(m)


    def delete_element(self, measurement: DatabaseMeasurement):
        """deletes a single element from the database.

        Args:
            measurement (DatabaseMeasurement): measurement to be deleted from datatbase.
        """        

        self.curser.execute(f"DELETE FROM measurements WHERE tempera_id = {measurement.tempera_id} and timestamp = '{measurement.timestamp}'")
        self.connection.commit()

        logger.info("Element(s) deleted")

    def clear(self):
        """CLears the measurement table (all data)
        """

        self.curser.execute("DELETE FROM measurements")
        self.connection.commit()
        logger.info("measurement table cleared")
    