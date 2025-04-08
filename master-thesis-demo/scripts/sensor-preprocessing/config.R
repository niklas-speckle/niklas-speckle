################################################################################
## CONFIG ----------------------------------------------------------------------
## directory config ------------------------------------------------------------

# source for sensor data
data_src_dir_sensor <- file.path("../../data/sensor-data/")

# directory to save data to
data_save_dir <- file.path("../../data/")

# Filename for saving day/night aggregated data frame
df_aggregated_per_day_night_save <- "df_aggregated_per_day_night.rds" # contains two data frames: sleep and activity
df_aggregated_per_night_save <- "df_aggregated_per_night.csv"
df_aggregated_per_day_save <- "df_aggregated_per_day.csv"

# Filename for saving day/night aggregated data frame after outlier and missing value preprocessing
df_aggregated_per_day_night_nooutliers_save <- "df_aggregated_per_day_night_nooutliers.rds"
df_aggregated_per_night_nooutliers_save <- "df_aggregated_per_night_nooutliers.csv"
df_aggregated_per_day_nooutliers_save <- "df_aggregated_per_day_nooutliers.csv"


# Filename for saving data frame after `sensor_preprocessing.R`
sensor_df_filename_save <- "df_sensor.rds" 

# Filename for saving data frame after `styling.R`
df_complete_save <- "df_final.rds"

## outlier config --------------------------------------------------------------
# How many missing values are allowed per night for the aggregation of a single variable to be valid?`
# e.g. if more than 10% of the sensor values are missing for a night/day the aggregation is invalid and set to NA for this night
# IMPORTANT: this is checked for each single variable as the sensor might not detect HR but sleep - 
# thus, it is possible that f.e. HR is missing for a night but sleep is not
THRESHOLD_MISSING_PROPORTION <- 0.1

# fills short missing value sequences (when sensor could not detect wake/sleep) with same value as before and after
#e.g. 00000002222000000 -> 00000000000000000
#e.g. 00000002222100000 -> 00000002222100000
# How large can a missing sequence be in minutes?
LENGTH_MISSING_SEQUENCE_FILL_THRESHOLD <- 30
# How long should a uniform sequence be before filling the missing values inbetween? (i.e. in the example above: How long should the sequence of 0s be before and after the 2s?)
LENGTH_BEFORE_AND_AFTER_MISSING_SEQUENCE_FILL_THRESHOLD <- 30

# Drop unrealistic values
# lower and upper threshold for when a value is considered unrealistic and set to NA
# the ranges are inclusive - so the edge values of the range are not set to NA
SLEEP_DURATION_THRESHOLD <- c(60, 900) # in minutes
WAKE_DURATION_THRESHOLD <- c(0, 480) # wake minutes per night
NIGHT_HR_THRESHOLD <- c(25, 110) # in bpm
MOVAC_THRESHOLD <- c(-Inf, Inf) # do not know realistic values
PAM_THRESHOLD <- c(-Inf, Inf) # do not know realistic values
DAY_HR_THRESHOLD <- c(40, 140) # in bpm
SLEEP_04_DURAT_THRESHOLD <- c(SLEEP_DURATION_THRESHOLD[1]/60, SLEEP_DURATION_THRESHOLD[2]/60) # Sleep diary items in hours


# Drop nonsense values (some participants have more than 4 days with nonsense coding for sleep/wake)
# ATTENTION this is hard coded - specify which participants and nights you would like to delete in which measuremen (T1/T2)
PARTICIPANTS_AND_NIGHTS_TO_REMOVE <- list(
  "ids" = c(1453),
  "nights" = list("1453" = "all"), # had to be removed due to medical issues
  "MeasurementTime" = list("1453" = c("T1", "T2"))
)







# Additional helper functions for saving data frames ###########################


#' gets the basename of a file without extension
#' 
#' @param filename: the filename with extension
#' 
#' @return: the basename of the file without extension
get_basename <- function(filename){
  basename <- strsplit(filename, "\\.")[[1]][1]
  return(basename)
}




