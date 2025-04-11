from datetime import datetime
import asyncio
import schedule  
from database.database_service import DatabaseService
from database.data_classes import DatabaseMessage
import ble_connection.ble_service as ble
import rest.rest_service as rest

# logger setup
from log.log_service import log_setup, delete_old_log_file
logger = log_setup(__name__)

# configuration of AP
from config.configuration import ap_configuration
AP_ID = f"{ap_configuration.group}-AP-{ap_configuration.id}"


async def startup():
    """This finction shall be run at startup to log/send a message to the server that AP has restarted.
    It also sends all currently saved data to the server and checks for updates on TD status.
    """

    logger.info(f"{AP_ID} started at {datetime.now()}")
    database = DatabaseService()
    database.messages.save(DatabaseMessage(datetime.now(),"AP",ap_configuration.id,1,f"{AP_ID} started at {datetime.now().strftime(ap_configuration.time_format_in_messages)}"))
    database.tempera_devices.diconnect_all() # no device can be connected when restarting
    delete_old_log_file()



##################
# run main files #
##################

async def main():
    """Main file executing startup and the standard connections (ble and rest) and their intervals.
    """

    await startup()

    database = DatabaseService()

    while True:
        schedule.run_pending()
        rest.send_messages(database)
        rest.send_measurements(database)

        rest.receive_updates(database) # check for TD updates before ble connection
        await ble.get_data_from_enabled()
        rest.send_time_records(database)
        await ble.check_not_registered()

        #Wait for a certain period before looping again
        await asyncio.sleep(ap_configuration.connection_intervall)

        # loop to wait for connection_factor between rest and ble (measurements)
        for i in range(ap_configuration.connection_factor_rest - 1):
            schedule.run_pending()
            rest.receive_updates(database) #check for TD updates           
            await ble.get_data_from_enabled()
            rest.send_time_records(database)
            await ble.check_not_registered()
            await asyncio.sleep(ap_configuration.connection_intervall)
        


schedule.every().day.at("00:00").do(delete_old_log_file)
asyncio.run(main())