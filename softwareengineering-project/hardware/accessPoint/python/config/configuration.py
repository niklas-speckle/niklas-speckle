import yaml
from log.log_service import log_setup,CONFIG_PATH

logger = log_setup(__name__)

class Configuration:
    """Configuration class that saves all the configurations of the config file
    """    

    def __init__(self):
        """Generates Configuration object from conf.yaml
        """        

        config = open_conf()
        if config != None:
            self.group = config.get("group")
            self.id = config.get("id_accesspoint")
            self.ip_server = config.get("ip_server")

            #log_file_path is extracted seperately in log_service (circular import!)

            self.connection_intervall = config.get("connection_intervall")
            self.connection_factor_rest = config.get("connection_factor_rest")
            self.request_timeout = config.get("request_timeout")
            self.ble_timeout = config.get("ble_timeout")

            self.time_conversion_server = config.get("time_conversion_server")
            self.time_conversion_tempera_devices_server = config.get("time_conversion_tempera_devices_server")
            self.time_format_in_messages = config.get("time_format_in_messages")

            self.td_enabled = config.get("tempera_status_enabled")
            self.td_disabled = config.get("tempera_status_disabled")
            self.td_not_registered = config.get("tempera_status_not_registered")

            self.td_list_created = config.get("tempera_list_update_created")
            self.td_list_updated = config.get("tempera_list_update_updated")
            self.td_list_deleted = config.get("tempera_list_update_deleted")

            self.work_mode_available = config.get("work_mode_available")
            self.work_mode_meeting = config.get("work_mode_meeting")
            self.work_mode_deep_work = config.get("work_mode_deep_work")
            self.work_mode_out_of_office = config.get("work_mode_out_of_office")

            self.post_address = config.get("post_address_http") + self.ip_server + config.get("post_address_port")
            self.post_address_message = self.post_address + config.get("rest_message_address")
            self.post_address_measurement = self.post_address + config.get("rest_measurement_address")
            self.post_address_time_record = self.post_address + config.get("rest_time_record_address")
            self.post_address_get = self.post_address + config.get("rest_get_address")

            self.uuid_measurement_service = config.get("uuid_measurement_service")
            self.uuid_work_modus_service = config.get("uuid_work_modus_service")
            self.uuid_work_modus = config.get("uuid_work_modus")
            self.uuid_modus_changed = config.get("uuid_modus_changed")
            self.uuid_read_write_data = config.get("uuid_read_write_data")

            self.uuid_device_name = config.get("uuid_device_name")
            self.uuid_temperature = config.get("uuid_temperature")
            self.uuid_humidity = config.get("uuid_humidity")
            self.uuid_light_intensity = config.get("uuid_light_intensity")
            self.uuid_air_quality = config.get("uuid_air_quality")


            logger.info("Configuration successful")

        else: # still necessary?
            logger.critical("Configuration not successful")
            raise Exception("Failed to read configuration! Please Check conf.yaml as well as the import path of conf.yaml")

        self.time_conversion_ap = '%Y-%m-%d %H:%M:%S.%f'

    def __str__(self):
        string = f"group: {self.group}, id: {self.id}, id_server: {self.ip_server}"
        string += f"\n time conversion server: {self.time_conversion_server}\n time conversion access point: {self.time_conversion_ap}"
        return string
        



def open_conf():
    """Opens the file conf.yaml as a stream

    Returns:
        Stream: stream to access data from config file
    """

    #with open("/accessPoint/config_files/conf.yaml", 'r') as stream: #for raspberry
    with open(CONFIG_PATH, 'r') as stream:
        try:
            return yaml.safe_load(stream)
        except yaml.YAMLError as exc:
            logger.critical("Configuration error: " + exc)
            raise Exception("Configuration error: " + exc)
            



# configuration that can be called from other files
ap_configuration = Configuration()
