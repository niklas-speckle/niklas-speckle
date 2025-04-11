import logging
import yaml
from datetime import date, timedelta
import os


# change to "config_files/conf.yaml" for Raspberry
CONFIG_PATH = "hardware/accessPoint/config_files/conf.yaml"


#########################
# info from config file #
#########################
# needs to be here to avert circular import!

def log_path():
    """gets path, where log-files shall be saved to from conf.yaml

    Returns:
        str: log-file-path
    """

    with open(CONFIG_PATH, 'r') as stream:
        try:
            return yaml.safe_load(stream).get("log_files_filepath")
        except yaml.YAMLError as exc:
            logger.critical("Configuration error: " + exc)
            raise Exception("Configuration error: " + exc)
        
def log_file_name():
    """gets name of log-files (the beginning before the timestamp) from conf.yaml ({group}-AP-{id_accesspoint}_). Defaults to default-AP-0.

    Returns:
        str: log-file name beginning
    """

    with open(CONFIG_PATH, 'r') as stream:
        try:
            config = yaml.safe_load(stream)
            id = config.get("id_accesspoint")
            return config.get("group") + f"-AP-{id}_"
        except yaml.YAMLError as exc:
            logger.critical("Configuration error: " + exc)
            raise Exception("Configuration error: " + exc)
        

def log_file_deletion_period():
    """gets number of days (after which log files shall be deleted) from conf.yaml

    Returns:
        int: number of days after which log files shall be deleted
    """

    with open(CONFIG_PATH, 'r') as stream:
        try:
            config = yaml.safe_load(stream)
            return config.get("log_file_deletion_period")
        except yaml.YAMLError as exc:
            logger.critical("Configuration error: " + exc)
            raise Exception("Configuration error: " + exc)
        


def delete_old_log_file():
    """deletes old log files that are older than a specific amount of days set in conf.yaml.
    """    

    days_to_delete = log_file_deletion_period()
    try:
        os.remove(log_path()+log_file_name()+ f"{date.today() - timedelta(days=days_to_delete) }.log")
        logger.info(f"Deleted log file from {date.today() - timedelta(days=days_to_delete) }")
    except FileNotFoundError:
        logger.info(f"No Log file to delete from {date.today() - timedelta(days=days_to_delete) }")



###########
# logging #
###########

def log_setup(name: str):
    """Sets up the logger for a file with the given name and ensures that all the loggers have the same format.

    Args:
        name (str): name the logger shall print when a log in that file is made (e.g. __name__)

    Returns:
        Logger: logger that can be used in the file
    """

    filename = log_path() + log_file_name() + f"{date.today()}.log"
    logging.basicConfig(filename=filename, level=logging.DEBUG, format="%(asctime)s  %(levelname)s: %(name)s: %(message)s") # in main?
    # , force=True could be necessary if file creation doesen't work properöy
    return logging.getLogger(name)

logger = log_setup(__name__)



