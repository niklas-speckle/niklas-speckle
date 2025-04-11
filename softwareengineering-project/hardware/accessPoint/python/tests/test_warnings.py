from database.database_service import DatabaseService
from database.data_classes import DatabaseMeasurement
import rest.rest_service as rest
import asyncio
from datetime import datetime

## This test file is used to trigger limit warnings at the backend
# if you want to check the sent e-mail too please enter it in pgAdmin first

database = DatabaseService() 
measurement_table = database.measurements

from log.log_service import log_setup
logger = log_setup(__name__)


TEMPERA_1 = 1


async def fast_low_temperature_test():
    """Sends measurements that have too low temperature, all the other measurements are inside the limits.
    The measurements are sent once per minute.
    """

    logger.info("Started Fast Low Temperature Test (infinite loop!)")
    while True:
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,15, 40, 16, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)


async def fast_double_air_light_test():
    """Air Quality too high and Light Intensity too low sumultaniously. Sent 1/min.
    """

    logger.info("Started Fast Double Air Light Test (infinity loop!)")
    while True:
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,22, 40, 105, 0))
        rest.send_measurements(database)
        await asyncio.sleep(60)


async def low_temperature_test():
    """Sends low temperature measurements but only every 5 min (like standard setting)
    """

    # sends only all 5 min
    logger.info("Started Low Temperature Test (infinity loop!)")
    while True:
        for i in range(5):
            measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,15, 40, 16, 800))
            await asyncio.sleep(60)
        rest.send_measurements(database)


async def alternating_temperature_test():
    """Sends one measurement per minute with different temperature values some of them underneath the limit and some above.
    """    

    logger.info("Started Alternating Temperature Test (infinity loop!)")
    while True:
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,15, 40, 16, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,16, 40, 16, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,20, 40, 15, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,21, 40, 16, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,19, 40, 16, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)


async def fast_high_humidity_test():
    """Sends Data with too high humidity values 1/min
    """

    logger.info("Started Fast High Humidity Test (infinity loop!)")
    while True:
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,22, 71, 15, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,22, 73, 15, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)
        measurement_table.save(DatabaseMeasurement(datetime.now(),TEMPERA_1,22, 75, 15, 800))
        rest.send_measurements(database)
        await asyncio.sleep(60)



###############################################################################################################
# Watch out!
#
# before starting these test please open pgAdmin and save your e-mail adress in user2 - you have to close the 
# table and click on save!
# If you run spring anew you have to restart pgAdmin and save your e-mail adress once more for the test to work 
# properly
################################################################################################################

def test_warnings():
    """Function runnging different tests for sending measurements to the webserver that will generate warnings.
    """

    logger.info("Started Test Warnings")

    asyncio.run(fast_high_humidity_test())
    print("\n")

    logger.error("Finished Test Warnings unexpectedly")