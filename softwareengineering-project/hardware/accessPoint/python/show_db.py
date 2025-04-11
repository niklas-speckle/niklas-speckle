from database.database_service import DatabaseService

from log.log_service import log_setup #import has to be written from directory that files are executed in
logger = log_setup(__name__)


## This file is used to show the current content of the database


logger.info("Show DB")
database = DatabaseService()

database.messages.print()
database.measurements.print()
database.time_records.print()
database.tempera_devices.print()