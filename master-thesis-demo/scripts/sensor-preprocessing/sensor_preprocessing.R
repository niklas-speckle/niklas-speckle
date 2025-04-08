#' @title Sensor Preprocessing
#' 
#' @description:
#' Extracts data from all single sensor files and aggregates them per day, night and measurement time T.
#' 
#' @note: requires all sensor files in a single folder given by `data_src_dir_sensor` in `config.R`
#' 
#' @return:
#' The data for day/night is saved to the data folder in: `df_aggregated_per_day_night.rds` (contains both day and night) or `df_aggregated_per_night.csv` / `df_aggregated_per_day.csv`
#' 
#' The aggreated data frame over T is saved in the data folder with the name given by `sensor_df_filename_save` in `config.R`
#' 
#' Additionally, the error files are saved in the data folder in `error_files.rds`. These are files where the data could not be extracted and thus are not included in the final data frame.

rm(list = ls())


source("config.R")
source("sensor_functions.R")


################################################################################
######### Actual Preprocessing begins here #####################################

files <- list.files(data_src_dir_sensor, recursive = TRUE, full.names = TRUE)

df_final <- vector("list", length = 2)
names(df_final) <- c("sleep", "activity")

error_files <- list()

for(i in seq_along(files)){
  print(sprintf("Processing File #%d", i))
  print(files[i])
  tryCatch(
    {
      df <- read_xlsx(files[i], guess_max = 100000) # max_guess increased to get correct data type as often >1000 rows are empty for single columns
    },
    error = function(e){
      # log error files with message
      error_files[files[i]] <<- e
      message(e)
    }
  )
  
  # create one data frame for each T to make preprocessing easier
  df_splitted_at_T <- split(df, df$MeasurementTime)
  
  for(i_T in seq_along(df_splitted_at_T)){
    current_T <- names(df_splitted_at_T)[i_T]
    current_df <- df_splitted_at_T[[i_T]]
    tryCatch(
      {
        print(sprintf("Processing %s", current_T))
        # extract per day / night
        df_extracted <- extract_data(current_df)
      
        # df per day for activity / night for sleep
        df_final$sleep <- rbind(df_final$sleep, df_extracted$sleep)
        df_final$activity <- rbind(df_final$activity, df_extracted$activity)
      },
      error=function(e) {
        # log error files with message
        error_files[files[i]] <<- e
        message(e)
      }
    )
  }
}


save(error_files, file = file.path(data_save_dir, "error_files.rds"))
save(df_final, file = file.path(data_save_dir, df_aggregated_per_day_night_save))
write.csv(df_final$sleep, file = file.path(data_save_dir, df_aggregated_per_night_save))
write.csv(df_final$activity, file = file.path(data_save_dir, df_aggregated_per_day_save))

source("outlire_missing_preprocessing_night-and-day-level.R")

load(file.path(data_save_dir, df_aggregated_per_day_night_nooutliers_save))

####### aggregate sleep over T

df_sleep <- df_final$sleep

# find for each participant if a weekday and weekend day is present to calculate weighted mean for sleep duration
df_sleep_weekend_aggregate <- df_sleep %>%
  group_by(Participant, MeasurementTime) %>%
  summarize(
    Participant = unique(Participant),
    MeasurementTime = unique(MeasurementTime),
    allowed_to_calculate_weighted_mean = is_allowed_to_calculate_mean(sleep_duration_per_night, weekend)
  ) %>% ungroup()


warning(sprintf("%i observations (T1/T2 level) had to be removed due to eather missing a weekday or weekend day. This meets %i participants.", 
                sum(!df_sleep_weekend_aggregate$allowed_to_calculate_weighted_mean),
                length(unique(df_sleep_weekend_aggregate$Participant[!df_sleep_weekend_aggregate$allowed_to_calculate_weighted_mean]))))
warning(sprintf("%i observations at T1 had to be removed due to eather missing a weekday or weekend day. This meets %i participants.", 
                sum(!df_sleep_weekend_aggregate$allowed_to_calculate_weighted_mean & df_sleep_weekend_aggregate$MeasurementTime == "T1"),
                length(unique(df_sleep_weekend_aggregate$Participant[!df_sleep_weekend_aggregate$allowed_to_calculate_weighted_mean & df_sleep_weekend_aggregate$MeasurementTime == "T1"]))))
warning(sprintf("%i observations at T2 had to be removed due to eather missing a weekday or weekend day. This meets %i participants.", 
                sum(!df_sleep_weekend_aggregate$allowed_to_calculate_weighted_mean & df_sleep_weekend_aggregate$MeasurementTime == "T2"),
                length(unique(df_sleep_weekend_aggregate$Participant[!df_sleep_weekend_aggregate$allowed_to_calculate_weighted_mean & df_sleep_weekend_aggregate$MeasurementTime == "T2"]))))


df_sleep_aggregated <- df_sleep %>%
  group_by(Participant, MeasurementTime) %>%
  summarize(
    Participant = unique(Participant),
    MeasurementTime = unique(MeasurementTime),
    mean_sleep_duration_per_night = weighted_mean_sleep(sleep_duration_per_night, weekend),
    mean_wake_duration_per_night = weighted_mean_sleep(wake_duration_per_night, weekend),
    number_of_nights_for_night_HR = sum(!is.na(mean_HR)),
    mean_HR = mean(mean_HR, na.rm = TRUE),
    number_of_nights_for_sleep = n(),
  ) %>% ungroup()

# join T1 and T2 of each participant to one row
df_sleep_final <- join_Ts(df_sleep_aggregated, c("Participant", "mean_sleep_duration_per_night", "mean_wake_duration_per_night", 
                                                 "mean_HR", "number_of_nights_for_sleep"))

####### aggregate activity over T

df_activity <- df_final$activity

df_activity_aggregated <- df_activity %>%
  group_by(Participant, MeasurementTime) %>%
  summarise(
    Participant = unique(Participant),
    MeasurementTime = unique(MeasurementTime),
    number_of_days_for_MovAcc = sum(!is.na(mean_MovAcc)),
    number_of_days_for_PaMAD = sum(!is.na(mean_PaMAD)),
    number_of_days_for_day_HR = sum(!is.na(mean_HR)),
    mean_MovAcc = mean(mean_MovAcc, na.rm = TRUE),
    mean_PaMAD = mean(mean_PaMAD, na.rm = TRUE),
    mean_HR = mean(mean_HR, na.rm = TRUE),
    mean_stress_last_5min = mean(mean_stress_last_5min, na.rm = TRUE),
    n_stress_last_5min = sum(n_stress_last_5min, na.rm = TRUE),
    mean_stress_self_02 = mean(mean_stress_self_02, na.rm = TRUE),
    n_stress_self_01 = sum(n_stress_self_01, na.rm = TRUE),
    mean_pregnancy_related_stress = mean(mean_preganancy_related_stress, na.rm = TRUE),
    n_pregnancy_related_stress = sum(n_pregnancy_related_stress),
    mean_pregnancy_related_support = mean(mean_pregnancy_related_support, na.rm = TRUE),
    n_pregnancy_related_support = sum(n_pregnancy_related_support),
    mdbf = mean(mdbf, na.rm = TRUE),
    mdbf_valence = mean(mdbf_valence, na.rm =TRUE),
    mdbf_arousal = mean(mdbf_arousal, na.rm = TRUE),
    mdbf_tiredness = mean(mdbf_tiredness, na.rm = TRUE),
    n_mdbf = sum(n_mdbf),
    mdbf_new = mean(mdbf_new, na.rm = TRUE),
    mdbf_valence_new = mean(mdbf_valence_new, na.rm =TRUE),
    mdbf_arousal_new = mean(mdbf_arousal_new, na.rm = TRUE),
    mdbf_tiredness_new = mean(mdbf_tiredness_new, na.rm = TRUE),
    n_mdbf_new = sum(n_mdbf_new),
    n_sleep_01_quality = sum(n_sleep_01_quality),
    sleep_01_quality = median(sleep_01_quality, na.rm = TRUE),
    n_sleep_02_03_durat = sum(!is.na(sleep_02_03_durat)),
    sleep_02_03_durat = weighted_mean_sleep(sleep_02_03_durat, weekend),
    n_sleep_04_durat = sum(!is.na(sleep_04_durat)),
    sleep_04_durat = weighted_mean_sleep(sleep_04_durat, weekend)
  ) %>% ungroup()

df_activity_final <- join_Ts(df_activity_aggregated, colnames_to_extract = 
                               c("Participant", "mean_MovAcc", "mean_PaMAD", "mean_HR", 
                                 "number_of_days_for_MovAcc", "number_of_days_for_PaMAD", "number_of_days_for_day_HR", 
                                 "mean_stress_last_5min", "n_stress_last_5min", "mean_stress_self_02","n_stress_self_01", 
                                 "mean_pregnancy_related_stress", "n_pregnancy_related_stress", 
                                 "mean_pregnancy_related_support", "n_pregnancy_related_support",
                                 "mdbf", "mdbf_valence", "mdbf_arousal", "mdbf_tiredness", "n_mdbf",
                                 "mdbf_new", "mdbf_valence_new", "mdbf_arousal_new", "mdbf_tiredness_new", "n_mdbf_new",
                                 "n_sleep_01_quality", "sleep_01_quality", "n_sleep_02_03_durat", 
                                 "sleep_02_03_durat", "n_sleep_04_durat", "sleep_04_durat"))

# make HR colname unique as "mean_HR.T1" "mean_HR.T2" is in df_activity_final and df_sleep_final
colnames(df_activity_final)[grep("mean_HR.*", colnames(df_activity_final))] <- c("mean_HR.day.T1", "mean_HR.day.T2")
colnames(df_sleep_final)[grep("mean_HR.*", colnames(df_sleep_final))] <- c("mean_HR.night.T1", "mean_HR.night.T2")

# join day and night data frame by participant
df <- full_join(df_sleep_final, df_activity_final, by = "Participant")
df <- df[,c(grep("Participant", colnames(df)), grep("*.T1", colnames(df)), grep("*.T2", colnames(df)))]
df$sleep_02_03_durat.T1 <- as.numeric(df$sleep_02_03_durat.T1)
df$sleep_02_03_durat.T2 <- as.numeric(df$sleep_02_03_durat.T2)

save(df, file = file.path(data_save_dir, sensor_df_filename_save))
df_basename = get_basename(sensor_df_filename_save)
write.csv2(df, file = file.path(data_save_dir, paste0(df_basename, ".csv")))


