import rest.rest_service as rest
from database.database_service import DatabaseService
from database.data_classes import DatabaseMessage

from datetime import datetime

from log.log_service import log_setup
logger = log_setup(__name__)



def test_messages():
    """This test is used to send messages to the backend to ckeck if the notifications are sent properly as well as if the auditing is working correctly and if the registration process of TD is working.      
    """

    database = DatabaseService()

    TEMPERA_1 = 1
    TEMPERA_100 = 100

    database.messages.save(DatabaseMessage(datetime.now(),"TD",TEMPERA_1,1,"Hello from TD 1 - i am enabled"))
    database.messages.save(DatabaseMessage(datetime.now(),"TD",TEMPERA_100,1,"Register me"))
    rest.send_messages(database)