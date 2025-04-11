import asyncio
from datetime import datetime

import database.database_service as dbs
from database.data_classes import DatabaseMeasurement

from log.log_service import log_setup
logger = log_setup(__name__)


async def testTimestamp():
    measurement_table = dbs.DatabaseService().measurements
    #while True: #still no duplicates for timestamps
    for i in range(20):
        m = DatabaseMeasurement(datetime.now(),3)
        print(m)
        measurement_table.save(m)
        measurement_table.print()
    measurement_table.clear() #delete elemets after test
        

def test_id():
    """This test is used to see if the creation timestamp of objects on the AP can be used as an id, even if the creation of objects is run simultaneously.
    """

    logger.info("Started Test ID")

    asyncio.run(testTimestamp())
    
    print("\n")

    logger.info("Finished Test ID")