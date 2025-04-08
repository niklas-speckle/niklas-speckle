#' @title PSQI Preprocessing
#' 
#' @description:
#' The script builds a data frame with the psqi global and subindexes.
#' 
#' @return:
#' output is saved to the file given by `psqi_df_filename_save` in config section below

rm(list = ls())

# CONFIG #######################################################################

# directory to save data to
data_save_dir <- file.path("../../data/")

# source data folder
data_src_dir <- file.path("../../data/psqi-data")

# source filenames for data frames - need to be .sav
psqi_filename <- file.path("psqi.csv")

# filename of saved data frame
psqi_df_filename_save <- "psqi_index.rds"

  
################################################################################

# Preprocessing Steps:
#
#   only take observations where fragen_zu_schlaf_und_schlafqualitt_complete == 2
#
#   psqi_2_mintosleep: set values > 500 to NA
#   psqi_4_hourssleep: set values >= 15 or < 1 to NA
#   psqi_10_sleepwithoralone: subtract 1
#   
#   psqi_1_bedtime: 
#     1. often time is between 08:00-12:59. It can be assumed that here the wrong time format was written. 
#     Thus, add 12 hours to these cases
# 
#   bedtime and timeawake:
#     In PSQI the date does not matter for both variables. It is just about the time. Thus, to calculate the average sleep duration
#     set the dates accordingly (see inline code documentation below).

library(lubridate)
library(dplyr)
source("psqi_functions.R")

psqi_df <- as.data.frame(read.csv(file.path(data_src_dir, psqi_filename)))

psqi_df <- psqi_df[psqi_df$fragen_zu_schlaf_und_schlafqualitt_complete == 2, ]

psqi_df$psqi_1_bedtime <- replace_empty_strings_with_NA(psqi_df$psqi_1_bedtime)
psqi_df$psqi_3_timeawake <- replace_empty_strings_with_NA(psqi_df$psqi_3_timeawake)

psqi_df$psqi_1_bedtime <- as.POSIXct(psqi_df$psqi_1_bedtime, format = "%Y-%m-%d %H:%M", tz = "UTC")
psqi_df$psqi_3_timeawake <- as.POSIXct(psqi_df$psqi_3_timeawake, format = "%Y-%m-%d %H:%M", tz = "UTC")

# if minutes to fall asleep >500 set to NA
max_mintosleep_idx <- which(psqi_df$psqi_2_mintosleep > 500)
psqi_df$psqi_2_mintosleep[max_mintosleep_idx] <- NA


## ------------------------------------------------------------------------------------------------------------
bedtime_lt <- as.POSIXlt(psqi_df$psqi_1_bedtime)
timeawake_lt <- as.POSIXlt(psqi_df$psqi_3_timeawake)

# I assume: often the bedtime is between 8 and 13. This might just be written in 12h format instead of 24h
# Thus if time is between 08-13 add 12h
bedtime_wrong_format <- which(bedtime_lt$hour >= 8 & bedtime_lt$hour <= 12)
bedtime_lt[bedtime_wrong_format] <- bedtime_lt[bedtime_wrong_format] + hours(12)

### Very often bedtime > timeawake. Wakeup and bedtime should not depend on date. Only on hours. Thus, set date for wakeuptime appropriately (same date or next day as bedtime)
# only do this step for reasonable bedtimes
# find reasonable wakeup and bedtimes
bedtime_reasonable <- bedtime_lt$hour >= 20 | bedtime_lt$hour <= 4
timeawake_reasonable <- timeawake_lt$hour >= 4 & timeawake_lt$hour <= 12
both_reasonable <- which(bedtime_reasonable & timeawake_reasonable)

for(i in both_reasonable){
  # set date for timeawake on the next day from bedtime (with the time from timeawake)
  if(bedtime_lt[i]$hour >= 20){
    timeawake_lt[i] <- 
      as.POSIXlt(paste(as.Date(bedtime_lt[i]) + 1, strftime(timeawake_lt[i], format = "%H:%M:%S")))
  } 
  # set date for timeawake on the same day from bedtime (with the time from timeawake)
  else if(bedtime_lt[i]$hour <= 4){
    timeawake_lt[i] <- 
      as.POSIXlt(paste(as.Date(bedtime_lt[i]), strftime(timeawake_lt[i], format = "%H:%M:%S")))
  }
}

one_unreasonable <- which(!bedtime_reasonable | !timeawake_reasonable)
# remaining unreasonable pairs which can not be fixed. 
data.frame(bedtime_lt[one_unreasonable], timeawake_lt[one_unreasonable])

# set those pairs as missing values
bedtime_lt[one_unreasonable] <- NA
timeawake_lt[one_unreasonable] <- NA

psqi_df$psqi_1_bedtime <- bedtime_lt
psqi_df$psqi_3_timeawake <- timeawake_lt


## set sleep hours >=15 or < 1 to NA
psqi_df$psqi_4_hourssleep[which(psqi_df$psqi_4_hourssleep >= 15)] <- NA
psqi_df$psqi_4_hourssleep[which(psqi_df$psqi_4_hourssleep < 1)] <- NA


# wrong coding
psqi_df$psqi_10_sleepwithoralone <- psqi_df$psqi_10_sleepwithoralone-1


## ------------------------------------------------------------------------------------------------------------
summary(psqi_df)

hist((psqi_df$psqi_1_bedtime-hours(12))$hour, breaks = 0:23)
hist(psqi_df$psqi_3_timeawake$hour, breaks = 0:23)


# styling
psqi_ordered_cols <- c(
  "psqi_1_bedtime",
  "psqi_2_mintosleep",
  "psqi_3_timeawake",
  "psqi_4_hourssleep",
  "psqi_5a_30min",
  "psqi_5b_wakeup",
  "psqi_5c_bathroom",
  "psqi_5d_cantbreathe",
  "psqi_5e_coughsnore",
  "psqi_5f_toocold",
  "psqi_5g_toohot",
  "psqi_5h_baddreams",
  "psqi_5i_pain",
  "psqi_5j_other",
  "psqi_5j_othdesc",
  "psqi_6_sleepqualoverall",
  "psqi_7_sleepmed",
  "psqi_8_troublestayingawake",
  "psqi_9_noenthusiasm",
  "psqi_10_sleepwithoralone",
  "psqi_10a_snoring",
  "psqi_10b_pausesbtwbreaths",
  "psqi_10c_legtwitching",
  "psqi_10d_confusion",
  "psqi_10e_otherrestlessness",
  "psqi_10e_otherdescribe"
)


# calculate psqi index with function from `psqi_functions.R`
psqi_index <- calculate_psqi_index(psqi_df[,psqi_ordered_cols])

str(psqi_index)

# categorize psqi score according to authors (values > 5 are qualified as bad sleep quality)
psqi_good_sleep_quality <- ifelse(psqi_index$psqi <= 5, 1, 0)

psqi_index$psqi_is_good_sleep_quality <- as.factor(psqi_good_sleep_quality)

## ------------------------------------------------------------------------------------------------------------
psqi_index <- as.data.frame(psqi_index)
psqi_index <-  cbind(psqi_index, psqi_df[,-which(colnames(psqi_df) %in% c("maternal_id"))])
psqi_index$id <- psqi_df$maternal_id
psqi_index$measurement <- psqi_df$redcap_event_name


# join psqi from T1 and T2 in wide format
psqi_index_splitted_at_T <- split(psqi_index, psqi_index$measurement)

if(length(psqi_index_splitted_at_T) != 2){
  stop("Splitting psqi data frame at `redcap_event_name` did not result in two data frames.")
}

psqi_index_final <- full_join(psqi_index_splitted_at_T$t1a_arm_1, psqi_index_splitted_at_T$t2a_arm_1, by = "id", suffix = c(".T1", ".T2"))

# styling
colnames_T1 <- colnames(psqi_index_final)[grep(".*\\.T1", colnames(psqi_index_final))]
colnames_T1 <- colnames_T1[!grepl("measurement", colnames_T1)]
colnames_T2 <- colnames(psqi_index_final)[grep(".*\\.T2", colnames(psqi_index_final))]
colnames_T2 <- colnames_T2[!grepl("measurement", colnames_T2)]


psqi_index_final <- psqi_index_final[,c("id", colnames_T1, colnames_T2)]



basename <- get_basename(psqi_df_filename_save)
save(psqi_index_final, file = file.path(data_save_dir, psqi_df_filename_save))
write.csv(psqi_index_final, file = file.path(data_save_dir, paste0(basename, ".csv")), row.names = FALSE)