#' @title Sensor Preprocessing per Day/Night
#' 
#' @description This script eliminates nights/days where less than 10% of the data were available. Additionally, unrealistic values are dropped
#' 
#' @return The resulting data frames is saved to the file given by `df_aggregated_per_day_night_nooutliers_save` in `config.R`. 

source("config.R")
source("outlier_preprocessing_functions.R")


load(file.path(data_save_dir, df_aggregated_per_day_night_save))
df_sleep <- df_final$sleep
df_activity <- df_final$activity

####################### sleep df ###############################################

# set aggregated values of nights with more than 10% missing data to NA

df_sleep$sleep_duration_per_night <- 
  set_missing_proportion_to_NA(df_sleep$sleep_duration_per_night, df_sleep$observations, df_sleep$not_wearn_or_NA_per_night)

df_sleep$wake_duration_per_night <- 
  set_missing_proportion_to_NA(df_sleep$wake_duration_per_night, df_sleep$observations, df_sleep$not_wearn_or_NA_per_night)

df_sleep$mean_HR <- 
  set_missing_proportion_to_NA(df_sleep$mean_HR, df_sleep$observations, df_sleep$missing_HR)

# drop unrealistic values

df_sleep$sleep_duration_per_night <- 
  drop_unrealistic(df_sleep$sleep_duration_per_night, SLEEP_DURATION_THRESHOLD[1], SLEEP_DURATION_THRESHOLD[2])

df_sleep$wake_duration_per_night <-
  drop_unrealistic(df_sleep$wake_duration_per_night, WAKE_DURATION_THRESHOLD[1], WAKE_DURATION_THRESHOLD[2])

df_sleep$mean_HR <-
  drop_unrealistic(df_sleep$mean_HR, NIGHT_HR_THRESHOLD[1], NIGHT_HR_THRESHOLD[2])

####################### activity df ############################################

# set aggregated values of days with more than 10% missing data to NA

df_activity$mean_MovAcc <- 
  set_missing_proportion_to_NA(df_activity$mean_MovAcc, df_activity$observations, df_activity$missing_MovAcc)

df_activity$mean_PaMAD <-
  set_missing_proportion_to_NA(df_activity$mean_PaMAD, df_activity$observations, df_activity$missing_PaMAD)

df_activity$mean_HR <-
  set_missing_proportion_to_NA(df_activity$mean_HR, df_activity$observations, df_activity$missing_HR)

# drop unrealistic values

df_activity$mean_MovAcc <- 
drop_unrealistic(df_activity$mean_MovAcc, MOVAC_THRESHOLD[1], MOVAC_THRESHOLD[2])

df_activity$mean_PaMAD <-
  drop_unrealistic(df_activity$mean_PaMAD, PAM_THRESHOLD[1], PAM_THRESHOLD[2])

df_activity$mean_HR <-
  drop_unrealistic(df_activity$mean_HR, DAY_HR_THRESHOLD[1], DAY_HR_THRESHOLD[2])

idx_realistic_sleep_duration <- between(df_activity$sleep_04_durat, SLEEP_04_DURAT_THRESHOLD[1], SLEEP_04_DURAT_THRESHOLD[2])
if(any(isFALSE(idx_realistic_sleep_duration))){
  warning(sprintf("%i days have an unrealistic sleep duration and are therefore set to NA", sum(idx_realistic_sleep_duration)))
}

df_activity$sleep_04_durat <-
  drop_unrealistic(df_activity$sleep_04_durat, SLEEP_04_DURAT_THRESHOLD[1], SLEEP_04_DURAT_THRESHOLD[2])

idx_missing_sleep_04_durat <- is.na(df_activity$sleep_04_durat)
table(df_activity$Participant[idx_missing_sleep_04_durat])


########## Here participants and nights are deleted as specified in config.R
for(id in PARTICIPANTS_AND_NIGHTS_TO_REMOVE$ids){
  nights <- PARTICIPANTS_AND_NIGHTS_TO_REMOVE$nights[[as.character(id)]]
  MeasurementTime <- PARTICIPANTS_AND_NIGHTS_TO_REMOVE$MeasurementTime[[as.character(id)]]
  
  if(is.character(nights) & all(nights == "all")){
    df_sleep <- df_sleep[!(df_sleep$Participant == id & df_sleep$MeasurementTime %in% MeasurementTime), ]
  } else {
    df_sleep <- df_sleep[!(df_sleep$Participant == id & df_sleep$MeasurementTime %in% MeasurementTime & df_sleep$night_counter %in% nights), ]
  }
}


df_final <- list(sleep = df_sleep, activity = df_activity)
save(df_final, file = file.path(data_save_dir, df_aggregated_per_day_night_nooutliers_save))
write.csv(df_final$sleep, file = file.path(data_save_dir, df_aggregated_per_night_nooutliers_save))
write.csv(df_final$activity, file = file.path(data_save_dir, df_aggregated_per_day_nooutliers_save))

