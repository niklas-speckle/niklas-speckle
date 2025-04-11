import asyncio
from bleak import BleakClient, BleakScanner, BleakGATTCharacteristic

from log.log_service import log_setup
logger = log_setup(__name__)

device_name = "G4T2-TD-1"


def button_notify_handler(characteristic, data):
    print("Workmode is: {0}".format(data.decode()))
    print(characteristic.uuid)

async def main():
    device = await BleakScanner.find_device_by_name(device_name) # could also have timeout
    if device is None:
        print("ERROR: Could not find device with name {0}".format(device_name))
        return ("ERROR: Could not find device with name {0}".format(device_name))
    print(device)
    # the naming convention is not intuitive imho
    async with BleakClient(device) as client:
        #print("Connected to device {0}".format(device_name))

        # print all services and all characteristics provided by device
        for service in client.services: # iterate all defined services on peripheral
            print("Serivce: {0}".format(service.uuid))
            for characteristic in service.characteristics: # print the characteristics of the service
                print("checking one characteristic")
                print("Characteristic: {0} \n\twith properties: {1}".format(characteristic.description, ", ".join(characteristic.properties)))
                try:
                    value = await client.read_gatt_char(characteristic.uuid)
                    print("characteristic.uuid {0} Value is: {1}".format(characteristic.uuid, value))
                    
                except Exception as e:
                    print("ERROR: reading characteristic {0}. Error is {1}".format(characteristic, e))

                
        print("================\n")



    print("INFO: Disconnected from device {0}".format(device_name))

def test_ble():
    """This test is used to see the data sent by the device G4T2-TD-1 via BLE.
    """

    logger.info("Started Test BLE")

    asyncio.run(main())
    
    print("\n")

    logger.info("Finished Test BLE")