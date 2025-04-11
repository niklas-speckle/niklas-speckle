import rest.rest_service as rest
import database.database_service as dbs

from log.log_service import log_setup
logger = log_setup(__name__)


def test_rest():
    """This test is used to test the rest connection of AP and backend (sends all elements currently saved in the database to backend)
    """

    logger.info("Started Test Rest")

    ################
    # GET REQUESTS #
    ################


    database = dbs.DatabaseService()

    database.tempera_devices.print()
    rest.receive_updates(database)


    #################################################
    # with to_send_data (the new rest converter)
    ################################################

    database.measurements.print()
    database.time_records.print()

    print("\nSend Measurements new:")
    rest.send_measurements(database)

    print("\nSend Time Records:")
    rest.send_time_records(database)

    print("\nSend Messages:")
    rest.send_messages(database)

    database.measurements.print()
    database.messages.print()

    print("\n")
    logger.info("Finished Test Rest")






