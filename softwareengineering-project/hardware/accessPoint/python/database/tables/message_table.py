from __future__ import absolute_import
from ..data_classes import DatabaseMessage
from datetime import datetime
from config.configuration import ap_configuration

from log.log_service import log_setup #import has to be written from directory that files are executed in
logger = log_setup(__name__)


class MessageTable:
    """
    A class used for operations on the messages table in a given sqlite3 database.

    Attributes
    ----------
    connection
        connection to databese
    curser
        curser corresponding to the connection
    id
        id of this access point device

    Methods
    -------
    save(id, message)
        saves given message object into database
    get(message_type=None)
        gets all messages of specified type (or all if Null) that are saved in the databse
    print(message_type=None)
        prints all messages of specified type (or all if None) saved in the database
    delete_element(message)
        deletes a single element from the database
    clear()
        Clears the message table (all data)
    """


    def __init__(self, connection, curser):
        """sets up the message table in the given database

        Args:
            connection (Any): connection to databese
            curser (Any): curser corresponding to the connection

        message table details:
            device_type (str): type of device (AP or TD) the message is from
            device_id (int): id of device
            message_type (int): type of message (1 - info, 2 - warning, 3 - error)
            message (str): message recorded
            timestamp (datetime): timestamp of message recording
        """        

        self.connection = connection
        self.curser = curser

        self.curser.execute("CREATE TABLE IF NOT EXISTS messages (device_type TEXT, device_id INTEGER, message_type INTEGER, message TEXT, timestamp datetime)") 
        # shall we keep it? - or seperate log files?
        

    def save(self, message: DatabaseMessage):
        """Saves given message object into database

        Args:
            message (DatabaseMessage): message object to be saved in the message table of the database
        """        

        try:
            self.curser.execute(f"INSERT INTO messages VALUES  ('{message.device_type}', {message.device_id}, {message.message_type}, '{message.message}', '{message.timestamp}')")
            self.connection.commit()
        except AttributeError:
            logger.error("save: given object is not DatabaseMessage! Please Insert a DatabaseMessage object.")


    
    def get(self, message_type: int =None):
        """gets all messages of specified type (or all if Null) that are saved in the databse
        
        Args:
            message_type (int, optional): Message-type that will be fetched. Defaults to None, which fetches all messages.

        Returns:
            [DatabaseMessage]: list of saved messages
        """   

        if message_type == None:
            statement = "SELECT * FROM messages"
        else:
            statement = f"SELECT * FROM messages WHERE message_type = {message_type}"

        received_data = self.curser.execute(statement).fetchall()

        if received_data == []:
            logger.info("Empty get statement.")
    
        try:
            list_messages = []
            for data in received_data:
                list_messages.append(DatabaseMessage(datetime.strptime(data[4], ap_configuration.time_conversion_ap),data[0],data[1],data[2], data[3]))

            return list_messages
        except AttributeError:
            logger.error("get: given object is not DatabaseMessage! Please Insert a DatabaseMessage object.")
            return []


    def print(self, message_type: int =None):
        """Prints all saved messages for specified type (or all if Null)
        
        Args:
            message_type (int, optional): Message-type that shall be printed. Defaults to None, which prints all messages.
        """        

        messages = self.get(message_type=message_type)
        if message_type == None:
            print("\nAll messages:\n-------------")
        else:
            print(f"\nAll messages of type {message_type}:\n-----------------------")

        for m in messages:
            print(m)


    def delete_element(self, message: DatabaseMessage):
        """deletes a single element from the database.

        Args:
            message (DatabaseMessage): message to be deleted from datatbase.
        """  

        self.curser.execute(f"DELETE FROM messages WHERE device_type = '{message.device_type}' and device_id = {message.device_id} and timestamp = '{message.timestamp}'")
        self.connection.commit()

        logger.info("Element(s) deleted")


    def clear(self):
        """Clears the message table (all data)
        """

        self.curser.execute("DELETE FROM messages")
        self.connection.commit()
        logger.info("message table cleared")
