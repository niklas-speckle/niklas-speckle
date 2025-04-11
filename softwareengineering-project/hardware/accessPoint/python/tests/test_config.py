import config.configuration as conf

configuration = conf.ap_configuration

from log.log_service import log_setup #import has to be written from directory that files are executed in
logger = log_setup(__name__)


def show_config_data():
    """This test prints all the elements of ap_configuration (the configuration object generated when starting main) to see if they are initialized correctly.
    """

    logger.info("Started Test Config")
    
    print(f"group: {configuration.group}")
    print(f"id: {configuration.id}")
    print(f"ip_server: {configuration.ip_server}")

    print()
    print(f"connection_intervall: every {configuration.connection_intervall} s")
    print(f"connection_factor_rest: every {configuration.connection_factor_rest} intervals")
    print(f"request_timeout: every {configuration.request_timeout} s")
    print(f"ble_timeout: every {configuration.ble_timeout} s")

    print()
    print(f"time_conversion_server: {configuration.time_conversion_server}")
    print(f"time_conversion_ap: {configuration.time_conversion_ap}")
    print(f"time_conversion_tempera_devices_server: {configuration.time_conversion_tempera_devices_server}")
    print(f"time_format_in_messages: {configuration.time_format_in_messages}")

    print()
    print(f"post_address: {configuration.post_address}")
    print(f"post_address_message: {configuration.post_address_message}")
    print(f"post_address_measurement: {configuration.post_address_measurement}")
    print(f"post_address_time_record: {configuration.post_address_time_record}")
    print(f"post_address_get: {configuration.post_address_get}")

    print()
    print(f"td_enabled: {configuration.td_enabled}")
    print(f"td_disabled: {configuration.td_disabled}")
    print(f"td_not_registered: {configuration.td_not_registered}")

    print()
    print(f"td_list_created: {configuration.td_list_created}")
    print(f"td_list_updated: {configuration.td_list_updated}")
    print(f"td_list_deleted: {configuration.td_list_deleted}")

    print()
    print(f"work_mode_available: {configuration.work_mode_available}")
    print(f"work_mode_meeting: {configuration.work_mode_meeting}")
    print(f"work_mode_deep_work: {configuration.work_mode_deep_work}")
    print(f"work_mode_out_of_office: {configuration.work_mode_out_of_office}")

    print()
    print(f"uuid measurement service: {configuration.uuid_measurement_service}")
    print(f"uuid button service: {configuration.uuid_button_service}")
    print(f"uuid button status: {configuration.uuid_button_status}")
    print(f"uuid status changed: {configuration.uuid_status_changed}")
    print(f"uuid read data: {configuration.uuid_read_data}")

    print()
    print(f"uuid device name: {configuration.uuid_device_name}")
    print(f"uuid temperature: {configuration.uuid_temperature}")
    print(f"uuid humidity: {configuration.uuid_humidity}")
    print(f"uuid light intensity: {configuration.uuid_light_intensity}")
    print(f"uuid air quality: {configuration.uuid_air_quality}")

    print("\n")

    logger.info("Finished Test Config")