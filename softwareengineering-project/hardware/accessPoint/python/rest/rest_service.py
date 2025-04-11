import requests as rq

from rest.rest_classes import RestMessage, RestMeasurement, RestTimeRecord, RestTemperaDevice
from database.database_service import DatabaseService
from database.data_classes import DatabaseMessage, DatabaseMeasurement, DatabaseTimeRecord, DababaseTemperaDevice
from config.configuration import ap_configuration

from log.log_service import log_setup #import has to be written from directory that files are executed in
logger = log_setup(__name__)


ID = ap_configuration.group + f"-AP-{ap_configuration.id}"



#############
# Send Data #
#############

def send_messages(database: DatabaseService):
    """Sends all saved messages to the backend

    Args:
        database (DatabaseService): database themessages shall be retreived from.
    """

    message_table = database.messages
    adress = ap_configuration.post_address_message + ID

    send_data(message_table, adress,1)


def send_measurements(database: DatabaseService):
    """Sends all saved measurements to the backend

    Args:
        database (DatabaseService): database the measurements shall be retreived from.
    """

    measurement_table = database.measurements
    adress = ap_configuration.post_address_measurement + ID

    send_data(measurement_table, adress,2)


def send_time_records(database: DatabaseService):
    """Sends all saved time_records to the backend

    Args:
        database (DatabaseService): database the time records shall be retreived from.
    """

    time_record_table = database.time_records
    adress = ap_configuration.post_address_time_record + ID

    send_data(time_record_table, adress,3)
    


def send_data(table, post_adress: str, type: int):
    """Sends the data of the given table (messages, measurements or time records) to the backend.

    Args:
        table (MessageTable/MeasurementTable/TimeRecordTable): table of the database that the messages shall be sent from.
        post_adress (str): adress the post shall be made to
        type (int): type of data sent (messages:1, measurements:2, time_records:3)
    """

    to_send_data = table.get()

    if to_send_data == []:
        logger.info("send_data: no data to send")

    for to_send_element in to_send_data:
        if type == 1:
            data = convert_message_to_rest(to_send_element)
        elif type == 2:
            data = convert_measurement_to_rest(to_send_element)
        elif type == 3:
            data = convert_time_record_to_rest(to_send_element)
        else:
            logger.error("send_data: Conversion Error (type does not match the known conversion types)")
        to_send_json = vars(data)
        print(to_send_json)

        try:
            request = rq.post(post_adress, json=to_send_json, timeout=ap_configuration.request_timeout)
            status_code = request.status_code

            if status_code == 201 or status_code == 200:
                logger.info(f"send_data: transmission successful - {post_adress}")
                table.delete_element(to_send_element)

            elif status_code == 401:
                logger.warning(f"send_data: transmission failed due to unauthorized request: " + request.text + " - element will be deleted")
                logger.info("unauthorized transmission details: " + str(to_send_element))
                table.delete_element(to_send_element)

            else:
                logger.error(f"send_data: transmission failed - status code {status_code}: {request.text} - {post_adress}")
            
            logger.debug(f"send_data: json: {to_send_json}")

        except rq.exceptions.ConnectionError:
            logger.error(f"send_data: Connection Error: no connection to host {post_adress}")
            break




def receive_updates(database: DatabaseService):
    """This function gets updates (tempera device list) from the rest server and updates the local database.

    Args:
        database (DatabaseService): database the updates shall be saved in.
    """

    adress = ap_configuration.post_address_get + ID
    while True:
        try:
            answer = rq.get(adress, timeout=ap_configuration.request_timeout) 

            status_code = answer.status_code
            if status_code == 200: #successful get
                logger.info("receive_updates: get successful")
                update_database(database, answer)
                
                delete_req =rq.delete(adress, timeout=ap_configuration.request_timeout)
                if delete_req.status_code != 200:
                    logger.error(f"receive_updates: delete not successful - staus code {delete_req.status_code}")
                else:
                    logger.info("receive_updates: delete successful")
            else: 
                if status_code != 204: #nothing left to get
                    logger.error(f"receive_updates: get request failed - status code {status_code}")
                else:
                    logger.info("receive_updates: no updates")
                break

        except rq.exceptions.ConnectionError:
            logger.error("receive_updates: Connection Error: no connection to host")
            logger.debug(f"get adress: {adress}")
            break


def update_database(database: DatabaseService, request_answer):
    """This function updates the database given the response of the rest_get method.

    Args:
        database (DatabaseService): Database where the update shall be made.
        request_answer (rest response): Answer of the get request containing the logStatus of the update as well as the current staus of the tempera_device.
    """    

    json = request_answer.json()
    logger.debug(f"update_database: received json: {json}")
    tempera_device = RestTemperaDevice(**json)

    if tempera_device.logStatus == ap_configuration.td_list_deleted:
        database.tempera_devices.delete(tempera_device.temperaDeviceId)
    elif tempera_device.logStatus == ap_configuration.td_list_updated or tempera_device.logStatus == ap_configuration.td_list_created:
        database.tempera_devices.save(convert_tempera_device_to_database(tempera_device))
    else:
        logger.error("update_database: unknown logStatus")
            

################
# Convert Data #
################

def convert_measurement_to_rest(database_measurement: DatabaseMeasurement):
    """Converts a database measurement object into the corresponding rest object of the same type

    Args:
        database_measurement (DatabaseMeasurement): database object (measurement) to convert

    Returns:
        RestMeasurement: rest object (measurement) needed for rest interaction
    """   

    dbm = database_measurement #to make it easiert to read
    return RestMeasurement(dbm.timestamp, dbm.tempera_id, dbm.temperature, dbm.humidity, dbm.air_quality, dbm.light_intensity)


def convert_time_record_to_rest(database_time_record: DatabaseTimeRecord):
    """Converts a database time record object into the corresponding rest object of the same type

    Args:
        database_measurement (DatabaseTimeRecord): database object (time record) to convert

    Returns:
        RestTimeRecord: rest object (time record) needed for rest interaction
    """ 

    dbtr = database_time_record
    return RestTimeRecord(dbtr.timestamp, dbtr.tempera_id, dbtr.work_mode)


def convert_message_to_rest(database_message: DatabaseMessage):
    """Converts a database message object into the corresponding rest object of the same type

    Args:
        database_measurement (DatabaseMessage): database object (message) to convert

    Returns:
        RestMessage: rest object (message) needed for rest interaction
    """    

    dbm = database_message
    return RestMessage(dbm.timestamp, dbm.device_type, dbm.device_id, dbm.message_type, dbm.message)


def convert_tempera_device_to_database(rest_tempera_device: RestTemperaDevice):
    """Converts a rest tempera device object into the corresponding database object.

    Args:
        rest_tempera_device (RestTemperaDevice): rest tempera device object to convert

    Returns:
        DatabaseTemperaDevice: database object (tempera_device) needed for storing in the database
    """    

    rtd = rest_tempera_device
    return DababaseTemperaDevice(rtd.temperaDeviceId, rtd.newStatus, rtd.timestamp)

