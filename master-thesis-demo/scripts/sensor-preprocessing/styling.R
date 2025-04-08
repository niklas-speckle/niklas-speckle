#' @title: Clean Style the Data Frame
#' 
#' @description:
#' The script cleans the data frame by renaming and reordering the columns and adding descriptions.
#' 
#' @return:
#' The cleaned data frame is saved to the file given by `df_complete_save` in `config.R`. With the same name a `.sav` and `.csv` file is saved.


rm(list = ls())

library("haven")
source("config.R")



load(file.path(data_save_dir, sensor_df_filename_save))

# Define variable replacements
replacements <- list(
  c("Participant", "id"),
  c("mean_sleep_duration_per_night", "sensor_sleep_duration"),
  c("mean_wake_duration_per_night", "sensor_wake_duration_during_night"),
  c("mean_HR.night", "HR_night"),
  c("number_of_nights_for_sleep", "#nights_sensor_sleep_duration"),
  c("number_of_days_for_MovAcc", "#days_MovAcc"),
  c("number_of_days_for_PaMAD", "#days_PaMAD"),
  c("number_of_days_for_day_HR", "#days_HR_day"),
  c("mean_MovAcc", "MovAcc"),
  c("mean_PaMAD", "PaMAD"),
  c("mean_HR.day", "HR_day"),
  c("mean_stress_last_5min", "stress_last_5min"),
  c("n_stress_last_5min", "#stress_last_5min"),
  c("mean_stress_self_02", "stress_self_triggered"),
  c("n_stress_self_01", "number_of_stressors_self_triggered"),
  c("mean_pregnancy_related_stress", "pregnancy_related_stress"),
  c("n_pregnancy_related_stress", "#pregnancy_related_stress"),
  c("mean_pregnancy_related_support", "pregnancy_related_support"),
  c("n_pregnancy_related_support", "#pregnancy_related_support"),
  c("n_mdbf", "#mdbf"),
  c("n_mdbf_new", "#mdbf_new"),
  c("n_sleep_01_quality", "#sleep_01_quality"),
  c("n_sleep_02_03_durat", "#sleep_02_03_durat"),
  c("n_sleep_04_durat", "#sleep_04_durat"),
  c("sleep_duration_psqi", "psqi_sleep_duration"),
  c("pss_sumscore", "pss"),
  c("cesd_score", "cesd"),
  c("mos_sumscore_missexcl", "mos_missexcl"),
  c("mos_sumscore_missincl", "mos_missincl")
)

# Loop through each replacement and perform column renaming
for (replacement in replacements) {
  old_col <- replacement[1]
  new_col <- replacement[2]
  df_nooutliers_col_indices <- grepl(old_col, colnames(df))
  affected_columns <- colnames(df)[df_nooutliers_col_indices]
  
  colnames(df) <- gsub(old_col, new_col, colnames(df))
}

# transform sensor sleep duration to hours
df$sensor_sleep_duration.T1 <- df$sensor_sleep_duration.T1/60
df$sensor_sleep_duration.T2 <- df$sensor_sleep_duration.T2/60
df$sensor_wake_duration_during_night.T1 <- df$sensor_wake_duration_during_night.T1/60
df$sensor_wake_duration_during_night.T2 <- df$sensor_wake_duration_during_night.T2/60

# reorder variables and add description.

# Variable names and descriptions
variable_names <- c(
  "id", "sensor_sleep_duration", "sensor_wake_duration_during_night", "HR_night", "psqi", "durat",
  "psqi_sleep_duration", "distb", "laten", "daydys", "hse", "slpqual", "meds", 
  "psqi_is_good_sleep_quality", "sleep_01_quality", "sleep_02_03_durat", "sleep_04_durat", 
  "MovAcc", "PaMAD", "HR_day", "bsa_movement_activity", "bsa_sport_activity", 
  "bsa_total_activity", "bsa_movement_activity_nowork", "bsa_total_activity_nowork", 
  "stress_last_5min", "stress_self_triggered", "number_of_stressors_self_triggered", 
  "pregnancy_related_stress", "pregnancy_related_support", "pss", "mdbf", 
  "mdbf_valence", "mdbf_arousal", "mdbf_tiredness", "mdbf_new", "mdbf_valence_new", 
  "mdbf_arousal_new", "mdbf_tiredness_new", "cesd", "mos_missexcl", "mos_missincl", "MDS",
  "ID_child", "TSratio", "TSratio_Ln"
)

# Create the description vector
variable_descriptions <- c(
  "id" = "maternal id.",
  "sensor_sleep_duration" = "Average sleep duration per night in hours",
  "sensor_wake_duration_during_night" = "Average wake duration per night in hours.",
  "HR_night" = "Average heart rate during night in beats per minute.",
  "psqi" = "Pittsburgh Sleep Quality Index global score. Higher values mean worse sleep.",
  "durat" = "PSQI subindex sleep duration.",
  "psqi_sleep_duration" = "Single item: Wie viele Stunden haben Sie in den letzten 30 Tagen nachts tatsächlich geschlafen?",
  "distb" = "PSQI subindex sleep disturbance.",
  "laten" = "PSQI subindex sleep latency.",
  "daydys" = "PSQI subindex day dysfunction due to sleepiness.",
  "hse" = "PSQI subindex sleep efficiency.",
  "slpqual" = "PSQI subindex overall sleep quality.",
  "meds" = "PSQI subindex needs meds to sleep.",
  "psqi_is_good_sleep_quality" = "global index > 5. Interpretation as bad sleep quality according to manual.",
  "sleep_01_quality" = "Single item: How would you rate the quality of your sleep last night?",
  "sleep_02_03_durat" = "wakeup time - fall asleep time (1 item each)",
  "sleep_04_durat" = "How many hours did you actually sleep last night?",
  "MovAcc" = "Average movement acceleration per day in m/s^2.",
  "PaMAD" = "Mean Amplitude Deviation.",
  "HR_day" = "Average heart rate during day in beats per minute.",
  "bsa_movement_activity" = "Bewegungs- und Sportaktivität Fragebogen. Daily activity (without sport) in minutes per week.",
  "bsa_sport_activity" = "Bewegungs- und Sportaktivität Fragebogen. Sport activity only. In minutes per week.",
  "bsa_total_activity" = "Bewegungs- und Sportaktivität Fragebogen. Total activity (sport + daily) in minutes per week.",
  "bsa_movement_activity_nowork" = "Bewegungs- und Sportaktivität Fragebogen. Daily activity (without sport and work) in minutes per week.",
  "bsa_total_activity_nowork" = "Bewegungs- und Sportaktivität Fragebogen. Total activity (sport + daily without work) in minutes per week.",
  "stress_last_5min" = "Average intensity of stress in the last 5 minutes (Item: Have you felt STRESSED in the last 5 minutes?)",
  "stress_self_triggered" = "Average intensity of stress self-triggered (Item: How stressful/difficult was that for you?)",
  "number_of_stressors_self_triggered" = "Average number of self triggered stressors.",
  "pregnancy_related_stress" = "Average intensity of pregnancy related stress items.",
  "pregnancy_related_support" = "Average intensity of pregnancy related support items.",
  "pss" = "Perceived Stress Scale global score.",
  "mdbf" = "Average intensity of mood disturbance items.",
  "mdbf_new" = "Average intensity of mood disturbance items. New scale (see MEKS EMA Codebook)",
  "cesd" = "Center for Epidemiologic Studies Depression Scale global score.",
  "mos_missexcl" = "Social Support Survey. Excluding missing values.",
  "mos_missincl" = "Social Support Survey. Including missing values.",
  "TSratio_Ln" = "Natural logarithm of TSratio.",
  "MDS" = "Mediterranean Diet Score."
)

reordered_variables <- c()

for(Tx in c("T1", "T2")){
  for(var in variable_names){
    current_var_name <- paste0(var, ".", Tx)
    if(current_var_name %in% colnames(df)){
      reordered_variables <- c(reordered_variables, current_var_name)
      if(var %in% names(variable_descriptions)){
            attr(df[[current_var_name]], "description") <- variable_descriptions[[var]]
      }
    }
  }
}
reordered_variables <- c("id", reordered_variables, colnames(df)[grepl("#", colnames(df))]) # "ID_child", "TSratio", "TSratio_Ln" missing in this demo
variables_df_noooutliers <- colnames(df)[!grepl("#", colnames(df))]


df <- df[, reordered_variables]

# rename # to n_ as # is invalid for spss
colnames(df) <- gsub("#", "n_", colnames(df))


# Save the data
basename <- get_basename(df_complete_save)


save(df, file = file.path(data_save_dir, paste0(basename, ".rds")))
write_sav(df, path = file.path(data_save_dir, paste0(basename, ".sav")))
write.csv(df, file = file.path(data_save_dir, paste0(basename, ".csv")))




