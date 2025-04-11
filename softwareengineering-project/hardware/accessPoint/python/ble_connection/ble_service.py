from datetime import datetime
from typing import Optional, List
#import asyncio
from bleak import BLEDevice, BleakClient, BleakScanner
import rest.rest_service as rest

#imports from own code
from config.configuration import ap_configuration
from database.database_service import DatabaseService
database = DatabaseService()



# logger setup
from log.log_service import log_setup
from database.data_classes import DababaseTemperaDevice, DatabaseMeasurement, DatabaseTimeRecord, DatabaseMessage
logger = log_setup(__name__)


async def read_sensor_data_from_client(client: BleakClient):
    ''''Gets a Bleak Client and reads all services and the service characteristics 
        if they match the given Characteristics they get stored 
        '''
    buttonread = 1
    workmoderead = 1
    sensordata = None
    for service in client.services:
        #read Messurment Data and set Read Messurement to true
        logger.info(f"Reading service {service.uuid}")
        if service.uuid == ap_configuration.uuid_measurement_service:
            sensordata = DatabaseMeasurement(datetime.now())
            logger.info("reading sensor data")
            logger.debug(f"service_characteristics {service.characteristics}")
            for characteristic in service.characteristics:
                try:
                    value = await client.read_gatt_char(characteristic.uuid)
                    logger.info(f"characteristic id read value {value}")
                    if(characteristic.uuid == ap_configuration.uuid_read_write_data):
                        logger.info("writing back to TD")
                        buttonread = int.from_bytes(value, "little")
                    else:
                        sensordata.set_sensor_data(value, characteristic.uuid)
                except Exception as e:
                    print(f"Error reading characteristic {characteristic}: {e}")
                    logger.error(f"Error reading characteristic {characteristic}: {e}")
            if buttonread == 0:
                try: 
                    await client.write_gatt_char(ap_configuration.uuid_read_write_data,b'\x01')
                    print("seted the datagotread charateristic")
                    logger.info("wrote to TD successfully")
                except Exception as e:
                    logger.error(f"Error writin characteristic {characteristic}: {e}")
        elif service.uuid == ap_configuration.uuid_work_modus_service:
            logger.info("reading work_mode")
            #read button Data and set BUTTON_STATUS_UUID to true, if it was false
            for characteristic in service.characteristics: # print the characteristics of the service
                #print("checking one characteristic")
                # print("Characteristic: {0} \n\twith properties: {1}".format(characteristic.description, ", ".join(characteristic.properties)))
                if(characteristic.uuid== ap_configuration.uuid_modus_changed):
                    try:
                        value = await client.read_gatt_char(characteristic.uuid)
                        workmoderead = int.from_bytes(value, "little")
                        print("characteristic.uuid {0} Value is: {1}".format(characteristic.uuid, workmoderead))
                    except Exception as e:
                        # print("ERROR: reading characteristic {0}. Error is {1}".format(characteristic, e))
                        logger.error(f"Error reading characteristic {characteristic}: {e}")
                if(characteristic.uuid == ap_configuration.uuid_work_modus):
                    try:
                        value = await client.read_gatt_char(characteristic.uuid)
                        work_mode = value.decode()
                        # print("characteristic.uuid {0} Value is: {1}".format(characteristic.uuid, work_mode))
                    except Exception as e:
                        # print("ERROR: reading characteristic {0}. Error is {1}".format(characteristic, e))
                        logger.error(f"Error reading characteristic {characteristic}: {e}")
            if workmoderead == 0:
                try: 
                    await client.write_gatt_char(ap_configuration.uuid_modus_changed,b'\x01')
                    print("the workmode charateristik got set to: gotread")
                except Exception as e:
                    logger.error(f"Error writin characteristic {characteristic}: {e}")
                time_record = DatabaseTimeRecord(datetime.now(), sensordata.tempera_id ,work_mode)

        else:
            logger.error(f"No match for service uuid {service.uuid}")

    if (workmoderead == 0):      
        return sensordata, time_record
    else: 
        return sensordata, None
        
                
                
async def discover_devices(devices: List[DababaseTemperaDevice]):
    """This function is used to search for the given devices and return the corresponding ble devices

    Args:
        devices (List[DababaseTemperaDevice]): List of Tempera Devices from database

    Returns:
        [ble_devices]: List of ble_devices, that can be connected via ble
    """    

    scanner = BleakScanner()
    existing_bledevices = []

    for device in devices:
        try: 
            ble_device = await scanner.find_device_by_name(device.string_id, timeout= ap_configuration.ble_timeout)
            #check if device is found 
            if ble_device:
                if device.status == ap_configuration.td_enabled:
                    logger.info(f"found device {device.string_id}")
                    existing_bledevices.append(ble_device)
                    ## rest info connected to device with status enabled
                    if not device.connected:
                        database.messages.save(DatabaseMessage(datetime.now(),"TD",device.tempera_id,1,f"Connection to {ap_configuration.group}-TD-{device.tempera_id} has been successfully established at  {datetime.now().strftime(ap_configuration.time_format_in_messages)}."))
                        database.tempera_devices.device_connected(device.tempera_id) #sets device connected in database
                        rest.send_messages(database)
                elif device.status == ap_configuration.td_not_registered:
                    if device.connected == False:
                        ## rest info device with status NOT_REGISTERED found
                        logger.info(f"found device {device.string_id} with status NOT_REGISTERED")
                        database.messages.save(DatabaseMessage(datetime.now(),"TD",device.tempera_id,1,f" {ap_configuration.group}-TD-{device.tempera_id} has been successfully registered at {datetime.now().strftime(ap_configuration.time_format_in_messages)}."))
                        database.tempera_devices.device_connected(device.tempera_id) #sets device connected in database
                        rest.send_messages(database)
                else:
                    logger.error("Tried to connect to disabled device!")
            else:
                print(f"could not find device {device.string_id}")
                logger.error(f"could not find device {device.string_id}")
                if device.connected:
                    database.messages.save(DatabaseMessage(datetime.now(),"TD",device.tempera_id,3,f"{ap_configuration.group}-TD-{device.tempera_id} not reachable.  {datetime.now().strftime(ap_configuration.time_format_in_messages)}"))
                    database.tempera_devices.device_disconnected(device.tempera_id) #set device to disconnected
                    rest.send_messages(database)
        except Exception as e: 
            logger.error(f"Error with scanner for {device.string_id}: {e}")        

    return existing_bledevices

async def check_not_registered():
    """This function searches for Tempera Devices that are NOT_REGISTERED and sends a message if they have been found.
    """

    #search not registered devices 
    not_registered_database_devices: DababaseTemperaDevice = database.tempera_devices.get(status=ap_configuration.td_not_registered) #add connected = False
    if not_registered_database_devices != []:
        await discover_devices(not_registered_database_devices)
            

async def get_data_from_enabled():
    """This function gets the data from Tempera Devices that are ENABLED
    """

    enabled_database_devices: DababaseTemperaDevice = database.tempera_devices.get(status=ap_configuration.td_enabled)

    if enabled_database_devices != []:
        connected_tempera_devices = await discover_devices(enabled_database_devices)
            

        ## for device in missing_devices in ble_devices:
        ##      rest message 
        for device in connected_tempera_devices:
            try:
                async with BleakClient(device) as client: 
                    data, time_record = await read_sensor_data_from_client(client)
                    if data is not None:
                        database.measurements.save(data)
                        print(data)
                    else:
                        logger.error("Measurement Data is null")
                    if time_record is not None:
                        database.time_records.save(time_record)
                        print(time_record)
                await client.disconnect()
            except Exception as e:
                ## uncommened next line when status connected/disconneced is implemented:
                ## Rest message "we lost connection to device"
                ## set connected to false in database for device: 
                logger.error(f"Error with connection for {device}: {e}")
    else:
        logger.info("No enabled TD found in database")

