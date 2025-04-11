from datetime import datetime
from database.database_service import DatabaseService
from database.data_classes import DababaseTemperaDevice
from config.configuration import ap_configuration

def test_connected():
    """This test checks if the connected attribute of DatabaseTemperaDevices is working properly.
    This attribute is needed for the reliability of BLE connections, that disonnected/ connected messages only appear once and not every minute.
    """    

    database = DatabaseService()

    TEMPERA_1 = 1
    database.tempera_devices.clear()
    database.tempera_devices.save(DababaseTemperaDevice(TEMPERA_1,ap_configuration.td_enabled,datetime.strptime("2024-05-29 17:13:21.1234", ap_configuration.time_conversion_ap)))
    database.tempera_devices.print()
    database.tempera_devices.device_connected(TEMPERA_1)
    database.tempera_devices.print()
    database.tempera_devices.device_disconnected(TEMPERA_1)
    database.tempera_devices.print()