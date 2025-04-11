from datetime import datetime

import database.database_service as dbs
from database.data_classes import DatabaseMessage,DatabaseMeasurement,DatabaseTimeRecord,DababaseTemperaDevice
from config.configuration import ap_configuration

from log.log_service import log_setup
logger = log_setup(__name__)

ACCESSPOINT_ID = ap_configuration.id
TEMPERA_1 = 1
TEMPERA_2 = 2

def test_database():
    """This test is used to see if the database, as well as all the methods like save,delete etc. are working properly.
    This test will also fill up the database with different objects if you want to test the rest connection.
    """

    logger.info("Started Test Database")

    # first get connection and setup tables
    database = dbs.DatabaseService() 

    # write "Hello World" into DB
    database.messages.save(DatabaseMessage(datetime.now(), "TD",TEMPERA_1,1,"Hello World"))
    database.messages.save(DatabaseMessage(datetime.now(), "TD",TEMPERA_1,2,"Konnichiwa Sekai"))
    database.messages.save(DatabaseMessage(datetime.now(), "AP",ACCESSPOINT_ID,1,"Hello from AP"))
    message = DatabaseMessage(datetime.now(),"TD",TEMPERA_1,2,"Soon Deleted")
    database.messages.save(message)


    # retrieve from DB
    result = database.messages.get()
    print("Result: ")
    print(result[0])

    database.messages.print()

    database.messages.delete_element(message)

    database.messages.print()

    message1 = DatabaseMessage(datetime.now(), "AP",ACCESSPOINT_ID,1,"new message")
    database.messages.save(message1)
    database.messages.print()
    database.messages.delete_element(message1)
    database.messages.print()

    # for rest only use tempera 1 - there is only one TD registered at the server right now!
    measurement = DatabaseMeasurement(datetime.now(), TEMPERA_2,19.2,0.2,78.2,5.6)
    database.measurements.save(measurement)
    database.measurements.save(DatabaseMeasurement(datetime.now(), TEMPERA_2,15.2,0.24,38.2,4.6))
    measurement2 = DatabaseMeasurement(datetime.now(), TEMPERA_2,temperature=19.2,humidity=100,light_intensity=5.6, air_quality=None)
    database.measurements.save(measurement2)
    database.measurements.save(DatabaseMeasurement(datetime.now()))
    database.measurements.print()

    #tosend_data = database.measurements.update_status_to_send()
    #for data in tosend_data:
    #    print(data)

    measurement = DatabaseMeasurement(datetime.now(), TEMPERA_1,34.2,2.2,78.2,5.4)
    database.measurements.save(measurement)
    # test wrong input
    database.measurements.save(1)
    database.measurements.print()

    #database.measurements.update_status_to_received()
    database.measurements.delete_element(measurement) # if measurement of TD 1 is not deleted here then you can see the transmission error 
    # (limits not set)
    database.measurements.print()

    # work_modus: AVAILABLE, MEETING, DEEP_WORK, OUT_OF_OFFICE
    time_record = DatabaseTimeRecord(datetime.now(), TEMPERA_1,ap_configuration.work_mode_available)
    database.time_records.save(time_record) 
    database.time_records.save(DatabaseTimeRecord(datetime.now(), TEMPERA_2,ap_configuration.work_mode_meeting))
    database.time_records.print()
    database.time_records.delete_element(time_record)
    database.time_records.print()


    time_record2 = DatabaseTimeRecord(datetime.now(), TEMPERA_1,ap_configuration.work_mode_out_of_office)
    database.time_records.save(DatabaseTimeRecord(datetime.now(), TEMPERA_1,ap_configuration.work_mode_available))
    database.time_records.save(DatabaseTimeRecord(datetime.now(), TEMPERA_1,ap_configuration.work_mode_meeting))
    database.time_records.save(time_record2)
    database.time_records.save(DatabaseTimeRecord(datetime.now(), TEMPERA_1,ap_configuration.work_mode_deep_work))
    database.time_records.delete_element(time_record2)
    database.time_records.print()

    # format numbers to 8 digits
    #print("{:08d}".format(1))
    #print("x"+"{:03d}".format(1) == "x001")




    data = database.measurements.get()
    print(data == [])
    database.tempera_devices.print()
    test_time = datetime.strptime("2024-04-18 10:00:21.1234", ap_configuration.time_conversion_ap)
    database.tempera_devices.save(DababaseTemperaDevice(TEMPERA_2,ap_configuration.td_not_registered,test_time))
    database.tempera_devices.print()

    database.tempera_devices.delete(2)
    database.tempera_devices.print()

    test_time = datetime.strptime("2024-04-18 10:03:11.1234", ap_configuration.time_conversion_ap)
    database.tempera_devices.save(DababaseTemperaDevice(TEMPERA_1,ap_configuration.td_enabled,test_time))
    database.tempera_devices.device_connected(TEMPERA_1)
    database.tempera_devices.print()

    database.tempera_devices.diconnect_all()
    database.tempera_devices.print()

    print("\n")

    logger.info("Finished Test database")