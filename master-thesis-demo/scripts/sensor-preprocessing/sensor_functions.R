library(dplyr)
library(lubridate)
library(readxl)
library(dplyr)
library(roxygen2)
library(todor)
library(BBmisc)
library(purrr)
library(car)

# stress variables to extract from raw data files
stress_vars <- c("b_stress_a_01_MovisensXS", "b_stress_b_01_MovisensXS", "b_stress_a_neu_01_MovisensXS", "stress_selbst_01_MovisensXS", "stress_selbst_02_MovisensXS")


#' Extracts data from a single raw sensor data file
#' 
#' 
extract_data <- function(df){
  check_str_of_df(df)
  
  # most MoviesenseXS columns are unnecessary for my analysis except stress variables
  df <- delete_MovisensXS_columns(df, c("Trigger_MovisensXS", stress_vars))
  
  # necessary as wake-up and fall asleep times should be determined for each da
  df$date_only <- as.Date(as.character(df$Date))
  
  # Some data frames have dates with few measures before and after the 4 days. I ignore those.
  df <- delete_dates_with_few_measures(df)
  
  # convert likert scales for stress_a_neu_01 as this uses a different likert scale as the stress_a_01. Variable ist not present in all data frames
  if(any(colnames(df) == "b_stress_a_neu_01_MovisensXS")){
    df$b_stress_a_neu_01_MovisensXS <- likert_convert(df$b_stress_a_neu_01_MovisensXS, 5, 0, 5, 1)
  }
  
  # calculate length of each sleep/wake/NA-phase (aka event) duration
  df$sleep_durations <- get_length_of_events(df$MeasurementTime, df$`NonWearSleepWake []_Sensor`, which_to_count = 1)
  df$wake_durations <- get_length_of_events(df$MeasurementTime, df$`NonWearSleepWake []_Sensor`, which_to_count = 0)
  df$sonsor_not_woren_or_NA <- get_length_of_events(df$MeasurementTime, df$`NonWearSleepWake []_Sensor`, which_to_count = c(2,NA)) # 2 or NA is equivalent
  
  # fill short non wear phases (30 minutes) to reduce missing values
  # e.g. 00000002222000000 -> 00000000000000000
  df$`NonWearSleepWake []_Sensor` <- 
    fill_short_missing_sequences(NonWearSleepWake = df$`NonWearSleepWake []_Sensor`, 
                                 sonsor_not_woren_or_NA = df$sonsor_not_woren_or_NA, 
                                 max_missing_duration = LENGTH_MISSING_SEQUENCE_FILL_THRESHOLD, 
                                 consecutive_before_and_after = LENGTH_BEFORE_AND_AFTER_MISSING_SEQUENCE_FILL_THRESHOLD)
  
  # find each fallasleep and wakeupt time
  df$fallasleep_times <- find_fallasleep_times(df$`NonWearSleepWake []_Sensor`)
  df$wakeup_times <- find_wakeup_times(df$`NonWearSleepWake []_Sensor`)
  
  # find final morning wakeup time for each unique day
  morning_triggers <- sapply(unique(df$date_only), function(x){
    get_final_morning_weakuptime(df$`NonWearSleepWake []_Sensor`, df$wake_durations, df$wakeup_times,
                                 as.POSIXlt(df$Date), df$date_only, x)
  })
  
  # The start time of the data frame varies (sometimes the df starts at 00.00, but sometimes it might start somewhere in the afternoon)
  # As my code tries to find wakeup times for each unique date, NA is produced as wake-up time for the first date in @seealso [get_final_morning_weakuptime]
  # This date should be omitted.
  morning_triggers <- remove_first_wakeup_time_if_na(morning_triggers, df$Date)
  
  # find final fallasleep for each unique day
  evening_triggers <- sapply(unique(df$date_only), function(x){
    get_final_evening_fallasleep_time(df$`NonWearSleepWake []_Sensor`, df$sleep_durations, df$fallasleep_times,
                                      as.POSIXlt(df$Date), df$date_only, x)
  })
  
  # The end time of the data frame varies (sometimes the df ends at 00.00, but sometimes it might end somewhere in the afternoon)
  # As my code tries to find fallasleep times for each unique date, NA might be produced as fall-asleep time for the last date in @seealso [get_final_evening_fallasleep_time]
  # This date should be omitted.
  evening_triggers <- remove_last_evening_fallasleep_if_na(evening_triggers, df$Date)
  
  # enumerate nights
  df$night_counter <- create_night_counter(as.POSIXlt(df$Date), morning_triggers, evening_triggers)
  
  # enumerate days
  df$day_counter <- create_day_counter(as.POSIXlt(df$Date), morning_triggers, evening_triggers)
  
  # extract sbs scores (pregnancy related stress)
  sbs_score <- get_d_sbs_scores(df)
  df$d_sbs <- sbs_score$sbs
  df$d_sbsu <- sbs_score$sbsu
  
  # extract mdbf scores (mood)
  mdbf_score <- get_mdbf(df)
  
  df <- cbind(df, mdbf_score$mdbf, mdbf_score$mdbf_new)
  
  df_extracted <- get_avg_sleep_and_activity(df)
  
  return(df_extracted)
}


#' Convert a Likert scale from one scale to another
#' 
#' This is used for converting data, for instance, from a 6-point scale to a 5-point scale.
#' The scale "dimensions" are specified in the function argument, and the function converts the numeric vector that is passed through. 
#' 
#' @param x Numeric vector to be passed through.
#' @param top.x Top value of the original scale. This would be 6 on a 0-6 scale.
#' @param bot.x Bottom value of the original scale. This would be 0 on a 0-6 scale.
#' @param top.y Top value of the new/target scale. This would be 5 on a 0-5 scale.
#' @param bot.y Bottom value of the new/target scale. This would be 0 on a 0-5 scale.
#' @keywords likert scale
#' @return A numeric vector containing the rescaled likert scale variable.
#' @examples 
#' data <-c(5, 4, 3, 2, 1)
#' likert_convert(data,5,0,10,0) # 5-point scale to 10-point scale
likert_convert <- function(x, top.x, bot.x, top.y, bot.y){
  y <- ((top.y-bot.y)*(x-bot.x)/(top.x-bot.x))+bot.y
  y
}

#' Reverse codes a Likert scale
#'
#' @param x is a scalar from or a vector of Likert scores to reverse code
#' @param min is the minimum value of the Likert scale
#' @param max is the maxium value of the Likert scale
#'
#' @examples
#' reverseCode(2, min = 1, max = 5) # reverse code "2" to a "4" on a Likert scale of 1-5
#' reverseCode(2:7, min = 2, max = 7) # reverse code a vector of scores on a Likert scale of 2-7
#' reverseCode(1, min = 0, max = 1) # reverse code binary response
reverse_code_likert_value <- function(x, min = 1, max = 5){
  
  # Written by Parker Tichko, May 2020
  # Email: my first name DOT my last name @ gmail.com
  
  if(min(x, na.rm = TRUE) < min | max(x, na.rm = TRUE) > max){
    warning("Warning: input is outside the range of the scale.")
  }
  
  sort(min:max, decreasing = TRUE)[x+(1-min)]
  
}

#' Reverse codes each likert value in a list of likert values
reverse_code_likert_vector <- function(x, min, max){
  sapply(x, reverse_code_likert_value, min = min, max = max)
}


#' check structure of df. More precisely, it checks wether nonWearSleepWake as main variable is present and if observations exist (nrow(df) != 0).
#' This is the case for some df.
check_str_of_df <- function(df){
  necessarry_colnames <- c("NonWearSleepWake []_Sensor")
  if(!necessarry_colnames %in% colnames(df)){
    stop(sprintf("%s missing in colnames.\n", necessarry_colnames))
  }
  if(nrow(df) == 0){
    stop("df has 0 rows.\n")
  }
}

#' Deletes unnecessary Moviesens columns
#' 
#' @param exceptions vector containing the variable names to hold anyway
#' 
#' @return returns the same data frame without Moviesense columns
delete_MovisensXS_columns <- function(df, exceptions){
  index_to_keep <- which(!grepl("*MovisensXS*", colnames(df)))
  index_to_keep <- sort(c(index_to_keep, which(grepl("d_sbs*", colnames(df))))) # include pregnancy related stress
  index_to_keep <- sort(c(index_to_keep, which(grepl("b_mdbf_a_*", colnames(df))))) # include feelings and mood
  index_to_keep <- sort(c(index_to_keep, which(grepl("sleep_*", colnames(df))))) # include sleep diary variables
  index_to_keep <- sort(c(index_to_keep, which(colnames(df) %in% exceptions))) # include exceptions
  return(df[,index_to_keep])
}


#' Some data frames have dates with few measures before and after the 4 days. I ignore those.
#' 
#' @return the df without all observations for dates with less than 200 measures (= 200 minutes)
delete_dates_with_few_measures <- function(df){
  
  dates_in_df <- table(df$date_only)
  dates_with_few_measures <- names(dates_in_df)[dates_in_df < 200]
  
  if(length(dates_with_few_measures) == 0){
    rval <- df
  } else{
    rval <- subset(df, !df$date_only %in% as.Date(dates_with_few_measures))
  }
  
  # check if only 4 unique dates are available for each T. Else message the user.
  unique_dates <- unique(rval$date_only)
  
  if(length(unique_dates) != 4){
    message("Number of unique dates per measurment time deviate from 4. \n")
  }
  
  return(rval)
}


#' Find wakeup times from NonWearSleepWake (transitions from 1 (= sleep) to 0 (= wake))
#' 
#' @param x column NonWearSleepWake
#' 
#' @return boolean vector of length x indicating wakeup times
find_wakeup_times <- function(x){
  lagged <- lag(x)
  wakeup_times <- (x == 0 & lagged == 1)
  wakeup_times <- replace(wakeup_times, is.na(wakeup_times), FALSE)
  return(wakeup_times)
}

#' Find fall asleep times from NonWearSleepWake (transitions from 0 (= wake) to 1 (= sleep))
#' 
#' @param x column NonWearSleepWake
#' 
#' @return boolean vector of length x indicating fall aspeel times
find_fallasleep_times <- function(x){
  fallasleep_times <- (x == 1 & lag(x) == 0)
  fallasleep_times <- replace(fallasleep_times, is.na(fallasleep_times), FALSE)
  return(fallasleep_times)
}


#' converts rle object to a vector of length(NonWeraSleepWake), where the duration of an event is saved at the beginning of each event. For all other rows, NA is filled.
convert_rle_to_vector_for_df <- function(NonWearSleepWake_rle){
  
  durations <- c()
  
  for(i in seq_along(NonWearSleepWake_rle$lengths)){
    current_length <- NonWearSleepWake_rle$lengths[i]
    current_value <- NonWearSleepWake_rle$values[i]
    if(current_value & !is.na(current_value)){
      durations <- c(durations, current_length, rep(NA,current_length-1))
    } else{
      durations <- c(durations, rep(NA, current_length))
    }
  }
  return(durations)
}



#' calculates duration of events e.g. sleep duration for each sleep period in minutes.
#' More precisely, the function counts the number of consecutive appearances of which_to_count and saves it at the index (corresponding to NonWearSleepWake)
#' where which_to_count first appears. This is done for each consecutive separately.
#' 
#' @param which_to_count the interval is calculated by summing up all contiguous appearances of which_to_count 
#' 1 = sleep, 0 = wake, 2 = non wear
#' 
#' @return returns a vector of length NonWearSleepWake, where duration is saved at the indices where consecutive starts.
get_length_of_events <- function(MeasurementTime, NonWearSleepWake, which_to_count = 1){
  
  NonWearSleepWake_rle <- rle(NonWearSleepWake %in% which_to_count)
  
  durations <- convert_rle_to_vector_for_df(NonWearSleepWake_rle)
  
  return(durations)
}


#' fills short missing value sequences (when sensor could not detect wake/sleep) with same value as before and after
#' e.g. 00000002222000000 -> 00000000000000000
#' e.g. 00000002222100000 -> 00000002222100000
#' 
#' @param sonsor_not_woren_or_NA return of get_length_of_events for not_woren_or_NA sequences
#' 
#' @param consecutive_before_and_after how many consecutive values (of the same number) before and after the missing sequence
#' 
#' @return returns an updated version of NonWearSleepWake where short missing sequences are filled with the same value as before and after
fill_short_missing_sequences <- function(NonWearSleepWake, sonsor_not_woren_or_NA, max_missing_duration, consecutive_before_and_after){
  
  # if no missing values return
  if(sum(!is.na(sonsor_not_woren_or_NA)) == 0){
    return(NonWearSleepWake)
  }
  
  # get start index of each NA-phase which is shorter than `max_missing_duration`
  startidx_of_NANs <- which(!is.na(sonsor_not_woren_or_NA))
  startidx_of_NANs <- startidx_of_NANs[sonsor_not_woren_or_NA[startidx_of_NANs] <= max_missing_duration]
  
  
  for(i in startidx_of_NANs){
    # check if uniform consecutive values before and after the missing sequence
    length_of_NA <- sonsor_not_woren_or_NA[i]
    
    tryCatch({
      # check if same sleep status is present before and after NA squence
      is_same_before_and_after <- isTRUE(all.equal(NonWearSleepWake[i-1], NonWearSleepWake[i+length_of_NA]))
      # check if there is a unifrom sequence of length `consecutive_before_and_after` before and after the NA sequence
      is_consecutive_before <- isTRUE(all.equal(NonWearSleepWake[(i-consecutive_before_and_after):(i-1)], rep(NonWearSleepWake[i-1], consecutive_before_and_after)))
      is_consecutive_after <- isTRUE(all.equal(NonWearSleepWake[(i+length_of_NA):(i+length_of_NA+consecutive_before_and_after-1)], rep(NonWearSleepWake[i-1], consecutive_before_and_after)))
    }, 
    # in case of index out of bounds keep NAs
    error = function(e){
      is_same_before_and_after <<- TRUE
      is_consecutive_before <<- TRUE
      is_consecutive_after <<- TRUE
    })

    if(!is_same_before_and_after | !is_consecutive_before | !is_consecutive_after){
      next()
    }
    
    # replace NA sequence with according value
    NonWearSleepWake[i:(i+length_of_NA)] <- NonWearSleepWake[i-1]
  }
  
  return(NonWearSleepWake)
}

#' finds final wakeup time. If wakup time could not be determined a default wakeup time of 07.00 is returned.
#' The wake up time is filtered such that 
#' - it is the first wake up between 04.00 and 12.00.
#' - and the wake duration is longer than 60min (to avoid wrong identifications of short wake ups during the night)
#' 
#' @param wake_durations a vector of length NonWearSleepWake, where the duration is 
#' saved at the indices where wake sequence starts (@seealso return of \code{\link{get_length_of_events}})
#' 
#' @param wakeup_times a vector of length NonWearSleepWake, where TRUE indicates a switch from 1 to 0 in NonWearSleepWake
#' 
#' @param date_lt list of length NonWearSleepWake of type POSIXlt
#' 
#' @param date_only vector of length NonWearSleepWake containing date without time
#' 
#' @param target_date date_only of which wakeup time should be found
#' 
#' @param earliest_wakeup At which hour can a day potentially start?
#' 
#' @param latest_wakeup Which is the latest wakeup to be considered?
#' 
#' @param awake_duration_threshold How long does one need to be awake to assume, that the day started.
#' e.g. if one falls asleep after 89 min again. This does not count as a definite start of the day.
#' 
#' @param default_wakeup_time if no wakeup time can be found this is taken as default wakeup time (this is mostly due to missing data).
#' By doing so one can consider the night anyway (even though there is only missing data).
#' 
#' @return the index of the found wakeup time in df. If none could be determined NA is returned
get_final_morning_weakuptime <- function(NonWearSleepWake, wake_durations, wakeup_times, date_lt, date_only, target_date, 
                                         earliest_wakeup = 4, latest_wakeup = 12, awake_duration_threshold = 90, default_wakeup_time = 7){
  
  # index of time window 04.00-12.00
  index_of_time_window_of_interest <- date_only == target_date & date_lt$hour >= earliest_wakeup & date_lt$hour < latest_wakeup
  
  is_full_time_window_available <- 
    any(date_only == target_date & date_lt$hour == earliest_wakeup) & any(date_only == target_date & date_lt$hour == latest_wakeup)
  
  if(!is_full_time_window_available){
    # if window not found - this happens if target date is present in df, but not the morning window (e.g. the df ends at 03.00)
    if(all(!index_of_time_window_of_interest)){
      message("Time window not available. NA for wakeup time returned.")
      return(NA)
    }
    # if one still sleeps at the end or if one is already awake at the beginning of an incomplete time window, then no wakup time can be determined
    # (this might be the case if the df began after earliest_wakeup or ended before last_wakeup for the day of interest).
    is_asleep_at_end <- all(sapply(tail(NonWearSleepWake[index_of_time_window_of_interest], 30) == 1, isTRUE)) # use isTRUE to handle NA values as well
    is_awake_at_beginning <- all(sapply(head(NonWearSleepWake[index_of_time_window_of_interest], 120) == 0, isTRUE))
    if(is_asleep_at_end | is_awake_at_beginning){
      message("is_asleep_at_end | is_awake_at_beginning evaluates to TRUE. NA returned.")
      return(NA)
    }
  }
  
  # find final wakeup time: wake duratioin at least 90. between 4-12.
  imputed_wake_up_index <- which.first(index_of_time_window_of_interest &
                                         # if no wake duration is available there is no wakeup time to be determined. Use lazy evaluation trick to avoid error
                                         (!is.na(wake_durations) & wake_durations >= awake_duration_threshold) & 
                                         wakeup_times)
  if(length(imputed_wake_up_index) == 0){
    # return default wakeup time only if full time window available
    index_7am <- which(date_only == target_date & date_lt$hour == default_wakeup_time & date_lt$min == 0)
    if(length(index_7am) == 0){
      message(sprintf("No wake up time or default wake up time found for %s. NA returned.", target_date))
      message(sprintf("This is date #%s in T.", which(unique(date_only) == target_date)))
      return(NA)
    }
    return(index_7am)
  } else {
    return(imputed_wake_up_index)
  }
}

#' The start time of the data frame varies (sometimes the df starts at 00.00, but sometimes it might start somewhere in the afternoon)
#' As my code tries to find wakeup times for each unique date, NA is produced as wake-up time for the first date in @seealso [get_final_morning_weakuptime]
#' This date should be omitted.
#' 
#' @param morning_triggers a list with index of each morning wake-up time#
#' 
#' @param dfDate vector with dates from the original df
remove_first_wakeup_time_if_na <- function(morning_triggers, dfDate){
  if(is.na(morning_triggers[1])){
    message("First morning trigger is removed, as it is NA.")
    message(sprintf("The first entry of the df is at: %s", dfDate[1]))
    return(morning_triggers[-1])
  } else {
    return(morning_triggers)
  }
} 


#' similar to get_final_morning_weakuptime. finds final fallasleep between 20.00-04.00. If no final fallasleep could be determined 23.00 is returned as default fall asleep.
#' the index is considered as final fall asleep if sleep duration is greater or equal to 30 minutes. So a short evening nap of 5 min is not considered as final fall asleep time.
#' 
#' @seealso [get_final_morning_weakuptime()]
get_final_evening_fallasleep_time <- function(NonWearSleepWake, sleep_durations, fallasleep_times, date_lt, date_only, target_date, 
                                              earliest_fallasleep = 20, latest_fallasleep = 4, sleep_duration_threshold = 30, default_fallasleep_time = 23){
  
  # index of time window 20.00-04.00
  index_of_time_window_of_interest <- 
    (date_only == target_date & date_lt$hour >= earliest_fallasleep) | ((date_only-1) == target_date & date_lt$hour <= latest_fallasleep)
  
  is_full_time_window_available <- 
    any(date_only == target_date & date_lt$hour == earliest_fallasleep) & any((date_only-1) == target_date & date_lt$hour == latest_fallasleep)
  
  if(!is_full_time_window_available){
    # if window not found - this happens if target date is present in df, but not the evening window (e.g. the df ends at 12.00)
    if(all(!index_of_time_window_of_interest)){
      message("Time window not available. NA for fallasleep time returned.")
      return(NA)
    }
    # if one is still awake at the end or if one is already asleep at the beginning of the incomplete time window
    # then no fallasleep time can be determined
    # (this might be the case if the df began after earliest_fallasleep or ended before latest_fallasleep)
    is_asleep_at_beginning <- all(sapply(head(NonWearSleepWake[index_of_time_window_of_interest], 30) == 1, isTRUE))
    is_awake_at_end <- all(sapply(tail(NonWearSleepWake[index_of_time_window_of_interest], 120) == 0, isTRUE))
    if(is_asleep_at_beginning | is_awake_at_end){
      message("is_asleep_at_beginning | is_awake_at_end evaluates to TRUE. NA returned.")
      return(NA)
    }
  }
  
  # find final fallasleep: wake duratioin at least 90. between 4-12.
  imputed_fallasleep_index <- which.first(index_of_time_window_of_interest & 
                                            (!is.na(sleep_durations) & sleep_durations >= sleep_duration_threshold) &
                                            fallasleep_times)
  
  if(length(imputed_fallasleep_index) == 0){
    index_11pm <- which(date_only == target_date & date_lt$hour == default_fallasleep_time & date_lt$min == 0)
    if(length(index_11pm) == 0){
      # This is very often the case for the last day in T as it ends at 00.00 and thus no fallasleep takes place for this date.
      # In this case do not print warning message, otherwise warn the user.
      unique_dates <- unique(date_only)
      is_last_date = unique_dates[length(unique_dates)] == target_date
      last_date <- tail(date_lt, 1)
      is_last_data_point_around_midnight <- last_date$hour < 24 |  last_date$hour %in% c(00,01,02)
      if(!(is_last_date & is_last_data_point_around_midnight)){
        message(sprintf("No fallasleep time or default fallasleep time found for %s. NA returned.", target_date))
        message(sprintf("This is date #%s out of %s dates in T.", which(unique_dates == target_date), length(unique_dates)))
      }
      return(NA)
    }
    return(index_11pm)
  } else{
    return(imputed_fallasleep_index)
  }
}

#' The end time of the data frame varies (sometimes the df ends at 00.00, but sometimes it might end somewhere in the afternoon)
#' As my code tries to find fallasleep times for each unique date, NA might be produced as fall-asleep time for the last date in @seealso [get_final_evening_fallasleep_time]
#' This date should be omitted.
remove_last_evening_fallasleep_if_na <- function(evening_triggers, dfDate){
  if(is.na(evening_triggers[length(evening_triggers)])){
    message("Last evening trigger is removed, as it is NA.")
    message(sprintf("The last entry of the df is at: %s", dfDate[length(dfDate)]))
    return(evening_triggers[-length(evening_triggers)])
  } else {
    return(evening_triggers)
  }
} 

#' checks if `morning_trigger` is greater than `evening_trigger` and if they are within 16h 
#' 
#' @param morning_trigger index of single morning trigger
#' @param evening_trigger index of single evening trigger
#' @param date_lt date vector of the whole data frame (all rows)
is_morning_and_evening_trigger_corresponding_for_night_counter <- function(morning_trigger, evening_trigger, date_lt){
  morning_trigger_date <- date_lt[morning_trigger]
  evening_trigger_date <- date_lt[evening_trigger]
  
  time_diff <- morning_trigger_date - evening_trigger_date
  
  # if evening trigger is greater than morning trigger
  if(time_diff < 0){
    sprintf("Evening Trigger: %s \n Morning Trigger: %s", evening_trigger_date, morning_trigger_date)
    message("evening trigger is greater than morning trigger.")
    return(FALSE)
  }
  
  # if triggers are too far apart (if they do not correspond to the same sleep phase (or day))
  if(as.numeric(time_diff, units = "hours") > 16){
    sprintf("Evening Trigger: %s \n Morning Trigger: %s", evening_trigger_date, morning_trigger_date)
    message("Triggers too far apart.")
    return(FALSE)
  }
  
  return(TRUE)
}

# necessary to do useful aggregations as one sleep phase might belong to two different days
# aggregation is then done over the night_counter, as one is only interested in night sleep
#' creates a vector of length = nrow(df) enumerating the number of nights for each row
#' A sleep phase is defined from each evening trigger to the next morning trigger
#' NA indicates day phases
#' 
#' @param date_lt list of type POSIXlt from df$Date
#' 
#' @param morning_trigger/evening_triggers vector containing the indices of final wake up and fall asleep times
#' 
#' @return vector of length nrow(date_lt) indicating the number of night per row. 
create_night_counter <- function(date_lt, morning_triggers, evening_triggers){
  
  night_counter <- c(rep(NA, length(date_lt)))
  
  # get all morning triggers after first evening trigger, as the df might begin with morning trigger
  # morning_triggers[morning_triggers > evening_triggers] cannot be done as NAs would be lost
  morning_triggers <- morning_triggers[which.first(morning_triggers > evening_triggers[1]):length(morning_triggers)]
  
  # morning trigger can only deviate by 1 (when df ends during last night - meaning a fallasleep occurred but no weak-up)
  if(length(morning_triggers) != length(evening_triggers)){
    morning_triggers <- c(morning_triggers, NA)
    # now length must be equal
    if(length(morning_triggers) != length(evening_triggers)){
      stop("create_night_counter: length(morning_triggers) != length(evening_triggers)\n")
    }
  }
  
  morning_evening_trigger_dyades <- data.frame(morning_triggers, evening_triggers)
  
  # only one missing morning-evening-trigger pair is expected when df ends during last night.
  # Otherwise, all morning evening pairs should be present, which is guaranteed by default fallasleep/wakeup
  morning_evening_trigger_dyades_with_missing_values <- which(apply(morning_evening_trigger_dyades, 1, anyNA))
  
  if(length(morning_evening_trigger_dyades_with_missing_values) >= 1){
    if(length(morning_evening_trigger_dyades_with_missing_values) > 1){
      stop("More than one missing morning-evening-trigger dyade.\n")
    }
    
    if(any(morning_evening_trigger_dyades_with_missing_values != nrow(morning_evening_trigger_dyades))){
      stop("Other than the last morning-evening-dyade is missing.\n")
    }
  }
  
  # ommit dyades with missing values as one cannot calculate total sleep night for that night
  morning_evening_trigger_dyades <- na.omit(morning_evening_trigger_dyades)
  
  # check if morning triggers are greater then corresponding evening triggers and if they are within 16h
  are_triggers_corresponding <- apply(morning_evening_trigger_dyades, 1, function(x) {is_morning_and_evening_trigger_corresponding_for_night_counter(x["morning_triggers"], x["evening_triggers"], date_lt)}, simplify = TRUE)
  if(!all(are_triggers_corresponding)){
    stop("create_night_counter: Not all morning evening trigger pairs are corresponding. Cannot create night counter.\n")
  }
  
  # enumerate nights
  for(i in 1:nrow(morning_evening_trigger_dyades)){
    night_counter[morning_evening_trigger_dyades$evening_triggers[i]:morning_evening_trigger_dyades$morning_triggers[i]] <- i
  }
  return(night_counter)
}

#' checks if `morning_trigger` is smaller than `evening_trigger` and if they are not within 4h or not greater apart than 24h
#' 
#' @param morning_trigger index of single morning trigger
#' @param evening_trigger index of single evening trigger
#' @param date_lt date vector of the whole data frame (all rows)
is_morning_and_evening_trigger_corresponding_for_day_counter <- function(morning_trigger, evening_trigger, date_lt){
  morning_trigger_date <- date_lt[morning_trigger]
  evening_trigger_date <- date_lt[evening_trigger]
  
  time_diff <- evening_trigger_date - morning_trigger_date
  
  # if morning trigger is after evening trigger
  if(time_diff < 0){
    sprintf("Evening Trigger: %s \n Morning Trigger: %s", evening_trigger_date, morning_trigger_date)
    message("evening trigger is before morning trigger.")
    return(FALSE)
  }
  
  # if triggers are too far apart or too close (if they do not correspond to the same day)
  if(as.numeric(time_diff, units = "hours") < 4 | as.numeric(time_diff, units = "hours") > 24){
    sprintf("Evening Trigger: %s \n Morning Trigger: %s", evening_trigger_date, morning_trigger_date)
    message("Triggers too close or too far apart.")
    return(FALSE)
  }
  
  return(TRUE)
}


#' Similar to create_night_counter but for day enumeration. Useful to do movement aggregation for days as one is only interested in 
#' daytime activity.
#' 
#' @param date_lt list of type POSIXlt from df$Date
#' 
#' @param morning_trigger/evening_triggers vector containing the indices of triggers
#' 
#' @return vector of length nrow(date_lt) indicating the number of night per row. 
create_day_counter <- function(date_lt, morning_triggers, evening_triggers){
  
  day_counter <- c(rep(NA, length(date_lt)))
  
  # get all evening triggers after first morning trigger, as the df might begin with evening trigger
  # evening_triggers[evening_triggers > morning_triggers] cannot be done as NAs would be lost
  evening_triggers <- evening_triggers[which.first(evening_triggers > morning_triggers[1]):length(evening_triggers)]
  
  # evening trigger can only deviate by 1 (when df ends during day - meaning a wake-up occurred but no fallasleep)
  if(length(morning_triggers) != length(evening_triggers)){
    evening_triggers <- c(evening_triggers, NA)
    
    # now length must be equal
    if(length(morning_triggers) != length(evening_triggers)){
      stop("create_day_counter: length(morning_triggers) != length(evening_triggers)\n")
    }
  }
  
  morning_evening_trigger_dyades <- data.frame(morning_triggers, evening_triggers)
  
  # only one missing morning-evening-trigger pair is expected when df ends during last day.
  # Otherwise, all morning evening pairs should be present.
  morning_evening_trigger_dyades_with_missing_values <- which(apply(morning_evening_trigger_dyades, 1, anyNA))
  
  if(length(morning_evening_trigger_dyades_with_missing_values) >= 1){
    if(length(morning_evening_trigger_dyades_with_missing_values) > 1){
      stop("More than one missing morning-evening-trigger dyade.\n")
    }
    
    if(any(morning_evening_trigger_dyades_with_missing_values != nrow(morning_evening_trigger_dyades))){
      stop("Other than the last morning-evening-dyade is missing.\n")
    }
  }
  
  # ommit missing dyades as activities cannot be aggregated for that day
  morning_evening_trigger_dyades <- na.omit(morning_evening_trigger_dyades)
  
  # check if morning and evening are corresponding
  are_triggers_corresponding <- apply(morning_evening_trigger_dyades, 1, function(x) {is_morning_and_evening_trigger_corresponding_for_day_counter(x["morning_triggers"], x["evening_triggers"], date_lt)}, simplify = TRUE)
  if(!all(are_triggers_corresponding)){
    stop("create_day_counter: Not all morning evening trigger pairs are corresponding. Cannot create day counter.\n")
  }
  
  # enumerate days
  for(i in 1:nrow(morning_evening_trigger_dyades)){
    day_counter[morning_evening_trigger_dyades$morning_triggers[i]:morning_evening_trigger_dyades$evening_triggers[i]] <- i
  }
  return(day_counter)
}

#' calculates single d_sbs score from a single row
calculate_d_sbs_score <- function(row_sbs, items_to_revers = c("d_sbs_01_MovisensXS")){
  
  for(item in items_to_revers){
    row_sbs[item] <- reverse_code_likert_value(row_sbs[[item]], min = 0, max = 5)
  }
  
  values <- row_sbs[1:(which(colnames(row_sbs) == "d_sbs_05_MovisensXS"))]

  # depending on question "Have you experienced any of the following symptoms since the last interview?"
  # If yes (coded as 0), take intensity from d_sbs_06_b_MovisensXS. else take 0 as value
  if(!is.na(row_sbs["d_sbs_06_a_0_MovisensXS"])){
    if(row_sbs["d_sbs_06_a_0_MovisensXS"] == 0){
      values <- unlist(c(values, row_sbs$d_sbs_06_b_MovisensXS))
    } else {
      values <- unlist(c(values, 0))
    }
  } else {
    values <- unlist(c(values, NA))
  }
  
  return(mean(values))
}

#' extracts d_sbs (pregnancy related stress) questionnaires scores from df
get_d_sbs_scores <- function(df, items_to_revers = c("d_sbs_01_MovisensXS")){
  
  if(is.list(df$d_sbs_01_MovisensXS)){
    missing_idx <- which(sapply(df$d_sbs_01_MovisensXS, length) == 0)
    df$d_sbs_01_MovisensXS[missing_idx] <- NA
    df$d_sbs_01_MovisensXS <- unlist(df$d_sbs_01_MovisensXS)
  }

  #two scales - social support and distress
  sbs_cols <- colnames(df)[grepl("d_sbs_", colnames(df))]
  sbsu_cols <- colnames(df)[grepl("d_sbsu_", colnames(df))]
  
  # d_sbs_00 and d_sbsu_00 does not contain information
  sbs_cols <- sbs_cols[!grepl("*_00_*", sbs_cols)]
  sbsu_cols <- sbsu_cols[!grepl("*_00_*", sbsu_cols)]
  
  rows_idx_sbs <- which(!is.na(df[,"d_sbs_01_MovisensXS"]))
  
  sbs_scores <- rep(NA, nrow(df))
  sbsu_scores <- rep(NA, nrow(df))
  
  # TODO: Why is number ranging from 0 to 5 (according to codebook it can only take 5 values)
  for(row_idx in rows_idx_sbs){
    sbs_scores[row_idx] <- calculate_d_sbs_score(df[row_idx,sbs_cols])
    sbsu_scores[row_idx] <- rowMeans(df[row_idx, sbsu_cols])
  }
  
  return(list("sbs"= sbs_scores, "sbsu" = sbsu_scores))
}

#' extracts mdbf (mood) questionnaire score from a single row
calculate_mdbf_scores <- function(row_mdbf){
  
  items_to_reverse <- grepl("01|03|05|08|10|12", colnames(row_mdbf))
  
  row_mdbf[1, items_to_reverse] <- t(sapply(row_mdbf[items_to_reverse], reverse_code_likert_value, min = 0, max = 5))
  
  scales <- c("mdbf_valence" = rowMeans(row_mdbf[1:4]), "mdbf_arousal" = rowMeans(row_mdbf[5:8]), "mdbf_tiredness" = rowMeans(row_mdbf[9:12]))
  scales <- c(scales, "mdbf" = mean(scales))
  return(scales)
}

#' calculates mdbf scores for whole df
get_mdbf <- function(df){
  mdbf_cols <- colnames(df)[grepl("b_mdbf_a", colnames(df))]
  
  # two times same questions
  mdbf_cols_new <- mdbf_cols[grepl("*_neu_*", mdbf_cols)]
  mdbf_cols <- setdiff(mdbf_cols,mdbf_cols_new)
  
  # 00 items do not contain information
  mdbf_cols_new <- setdiff(mdbf_cols_new, mdbf_cols_new[grepl("*_00_*", mdbf_cols_new)])
  mdbf_cols <- setdiff(mdbf_cols, mdbf_cols[grepl("*_00_*", mdbf_cols)])
  
  rows_idx_mdbf <- which(rowSums(is.na(df[,mdbf_cols])) != length(mdbf_cols))
  rows_idx_mdbf_new <- which(rowSums(is.na(df[,mdbf_cols_new])) != length(mdbf_cols_new))
  
  mdbf <- matrix(nrow=nrow(df), ncol=4)
  mdbf_new <- matrix(nrow=nrow(df), ncol=4)
  
  is_all_mdbf_NA <- all(colSums(is.na(df[,mdbf_cols])) == nrow(df))
  if(is_all_mdbf_NA){
    
    colnames(mdbf) <- c("mdbf_valence", "mdbf_arousal", "mdbf_tiredness", "mdbf")
    colnames(mdbf_new) <- paste0(colnames(mdbf), "_new")
    
    return(list(mdbf = mdbf, mdbf_new = mdbf_new))
  }
  
  for(row_idx in rows_idx_mdbf){
    val_tmp <- calculate_mdbf_scores(df[row_idx, mdbf_cols])
    mdbf[row_idx,] <- val_tmp
  }
  colnames(mdbf) <- names(val_tmp)
  for(row_idx in rows_idx_mdbf_new){
    val_tmp <- calculate_mdbf_scores(df[row_idx, mdbf_cols_new])
    mdbf_new[row_idx,] <- val_tmp
  }
  colnames(mdbf_new) <- paste0(names(val_tmp),"_new")
  
  return(list(mdbf = mdbf, mdbf_new = mdbf_new))
  
}

#' corrects possible user input errors and calculates sleep duration from daily sleep diaries
calculate_ema_sleep_duration <- function(fallasleep, wakeup){
  bedtime_lt <- as.POSIXlt(na.omit(fallasleep))
  timeawake_lt <- as.POSIXlt(na.omit(wakeup))
  
  if(length(bedtime_lt) == 0 | length(timeawake_lt) == 0){
    return(NA)
  }
  
  # I assume: often the bedtime is between 8 and 13. This might just be written in 12h format instead of 24h
  # Thus if time is between 08-13 add 12h
  is_bedtime_wrong_format <- bedtime_lt$hour >= 8 & bedtime_lt$hour <= 12
  
  if(is_bedtime_wrong_format){
    bedtime_lt <- bedtime_lt+ hours(12)
  }
  
  # Very often bedtime > timeawake. Wakeup and bedtime should not depend on date. Only on hours. Thus, set date for wakeuptime appropriately (same date or next day as bedtime)
  is_bedtime_reasonable <- bedtime_lt$hour >= 20 | bedtime_lt$hour <= 4
  is_timeawake_reasonable <- timeawake_lt$hour >= 4 & timeawake_lt$hour <= 12
  are_both_reasonable <- is_bedtime_reasonable & is_timeawake_reasonable
  
  if(are_both_reasonable){
    # set date for timeawake on the next day from bedtime (with the time from timeawake)
    if(bedtime_lt$hour >= 20){
      timeawake_lt <- 
        as.POSIXlt(paste(as.Date(bedtime_lt) + 1, strftime(timeawake_lt, format = "%H:%M:%S")))
    } 
    # set date for timeawake on the same day from bedtime (with the time from timeawake)
    else if(bedtime_lt$hour <= 4){
      timeawake_lt <- 
        as.POSIXlt(paste(as.Date(bedtime_lt), strftime(timeawake_lt, format = "%H:%M:%S")))
    }
 }
  
  sleep_duration <- timeawake_lt - bedtime_lt
  if(sleep_duration < 0){
    message("EMA sleep duration is negative. NA returned.")
    return(NA)
  }
  if(sleep_duration > 15){
    message("EMA sleep duration is greater than 15 hours. NA returned.")
    return(NA)
  }
  return(timeawake_lt - bedtime_lt)
}

#' calculates whether a date is a weekend day
is_weekend <- function(date){
  return(weekdays(date) == "Sonntag" | weekdays(date) == "Samstag")
}

#' aggregates the data frame to get mean sleep duration per night
get_nightly_avg <- function(df, group_variable = "night_counter"){
  
  df <- df[!is.na(df[group_variable]),]
  
  df_nightly_aggregated <- df %>%
    group_by(df[group_variable]) %>%
    summarize(
      Participant = unique(Participant),
      MeasurementTime = unique(MeasurementTime),
      night_counter = unique(get(group_variable)),
      sleep_duration_per_night = sum(`NonWearSleepWake []_Sensor` == 1, na.rm = TRUE),
      wake_duration_per_night = sum(`NonWearSleepWake []_Sensor` == 0, na.rm = TRUE),
      not_wearn_or_NA_per_night = sum(`NonWearSleepWake []_Sensor` %in% c(NA, 2), na.rm = TRUE),
      mean_HR = tryCatch({mean(`Hr [1/min]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      sd_HR = tryCatch({sd(`Hr [1/min]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      missing_HR = tryCatch({sum(is.na(`Hr [1/min]_Sensor`))},error = function(e) NA),
      observations = n(),
      fall_asleep_time = first(Date),
      wakeup_time = last(Date),
      weekend = is_weekend(last(Date))
    ) %>% ungroup()
  
  return(df_nightly_aggregated)
}


#' aggregates the data frame over days (i.e. mean activity by day)
get_daily_activity_avg <- function(df, group_variable = "day_counter"){
  
  df <- df[!is.na(df[group_variable]),]
  
  df_aggregated <- df %>%
    group_by(df[group_variable]) %>%
    summarize(
      Participant = unique(Participant),
      MeasurementTime = unique(MeasurementTime),
      day_counter = unique(get(group_variable)),
      # tryCatch: if for whatever reason a variable is missing or contains only NA just return NA
      mean_MovAcc = tryCatch({mean(`MovementAcceleration [g]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      sd_MovAcc = tryCatch({sd(`MovementAcceleration [g]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      missing_MovAcc = tryCatch({sum(is.na(`MovementAcceleration [g]_Sensor`))},error = function(e) NA),
      mean_PaMAD = tryCatch({mean(`PaMetricMeanAmplitudeDeviation [g]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      sd_PaMAD = tryCatch({sd(`PaMetricMeanAmplitudeDeviation [g]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      missing_PaMAD = tryCatch({sum(is.na(`PaMetricMeanAmplitudeDeviation [g]_Sensor`))},error = function(e) NA),
      mean_HR = tryCatch({mean(`Hr [1/min]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      sd_HR = tryCatch({sd(`Hr [1/min]_Sensor`, na.rm = TRUE)},error = function(e) NA),
      missing_HR = tryCatch({sum(is.na(`Hr [1/min]_Sensor`))},error = function(e) NA),
      cor_HR_MovAcc = tryCatch({cor(`Hr [1/min]_Sensor`, `MovementAcceleration [g]_Sensor`, use = "na.or.complete")},error = function(e) NA),
      cor_HR_PaMAD = tryCatch({cor(`Hr [1/min]_Sensor`, `PaMetricMeanAmplitudeDeviation [g]_Sensor`, use = "na.or.complete")},error = function(e) NA),
      mean_stress_last_5min = mean(dplyr::coalesce(b_stress_a_01_MovisensXS, b_stress_b_01_MovisensXS, b_stress_a_neu_01_MovisensXS), na.rm = TRUE),
      n_stress_last_5min = sum(!is.na(dplyr::coalesce(b_stress_a_01_MovisensXS, b_stress_b_01_MovisensXS, b_stress_a_neu_01_MovisensXS))),
      mean_stress_self_02 = mean(stress_selbst_02_MovisensXS[which(stress_selbst_01_MovisensXS == 1)], na.rm = TRUE),
      n_stress_self_01 = sum(stress_selbst_01_MovisensXS == 1, na.rm = TRUE),
      mean_preganancy_related_stress = mean(d_sbs, na.rm = TRUE),
      n_pregnancy_related_stress = sum(!is.na(d_sbs)),
      mean_pregnancy_related_support = mean(d_sbsu, na.rm = TRUE),
      n_pregnancy_related_support = sum(!is.na(d_sbsu)),
      n_mdbf = sum(!is.na(mdbf)),
      mdbf = mean(mdbf, na.rm = TRUE),
      mdbf_valence = mean(mdbf_valence, na.rm = TRUE),
      mdbf_arousal = mean(mdbf_arousal, na.rm = TRUE),
      mdbf_tiredness = mean(mdbf_tiredness, na.rm = TRUE),
      n_mdbf_new = sum(!is.na(mdbf_new)),
      mdbf_new = mean(mdbf_new, na.rm = TRUE),
      mdbf_valence_new = mean(mdbf_valence_new, na.rm = TRUE),
      mdbf_arousal_new = mean(mdbf_arousal_new, na.rm = TRUE),
      mdbf_tiredness_new = mean(mdbf_tiredness_new, na.rm = TRUE),
      sleep_01_quality = mean(sleep_01_MovisensXS, na.rm = TRUE),
      n_sleep_01_quality = sum(!is.na(sleep_01_MovisensXS)),
      sleep_02_03_durat = calculate_ema_sleep_duration(sleep_02_MovisensXS, sleep_03_MovisensXS),
      sleep_04_durat = ifelse(length(na.omit(sleep_04_MovisensXS)) == 0, NA, na.omit(sleep_04_MovisensXS)),
      wakeup_time = first(Date),
      weekend = is_weekend(first(Date)),
      fall_asleep_time = last(Date),
      observations = n()
    ) %>% ungroup()
  
  return(df_aggregated)
  
}

#' @return an aggregated form of df by day/night. 
get_avg_sleep_and_activity <- function(df, group_variables = c(day_counter = "day_counter", night_counter = "night_counter")){
  
  activity_agg <- get_daily_activity_avg(df, group_variables["day_counter"])
  
  sleep_agg <- get_nightly_avg(df, group_variables["night_counter"])
  
  list_names <- c(paste0("sleep_",group_variables["night_counter"]), paste0("activity_",group_variables["day_counter"]))
  
  rval <- list(sleep_agg, activity_agg)
  names(rval) <- list_names
  
  return(rval)
  
}

#' Is allowed to calculate weighted mean? i.e. is at least one weekday and one weekend day present for a single participant?
#' 
#' @param sleep_duration vector indicating sleep duration at the index position where fall asleep occurred (NA otherwise)
#' 
#' @param weekend vector indicating if index position is weekend day
#' 
#' @return TRUE if at least one weekday and one weekend day is present
is_allowed_to_calculate_mean <- function(sleep_duration, weekend){
  
  weekend <- weekend[!is.na(sleep_duration)]
  sleep_duration <- sleep_duration[!is.na(sleep_duration)]
  
  if(sum(!weekend) == 0 | sum(weekend) == 0){
    return(FALSE)
  } else {
    return(TRUE)
  }
  
}

#' calculate weighted means for sleep (weekdays should be weighted 5/7 and weekend 2/7)
#' Function to be used in dplyr::summarize
#' 
#' @note If not at least 1 weekday and 1 weekend day is present the mean sleep is NA
#' 
#' @param sleep_duration_per_nigth vector of sleep durations for each night
#' 
#' @param weekdays logical vector indicating if wakeup day of corresponding sleep_duration_per_night is weekend
weighted_mean_sleep <- function(sleep_duration_per_night, weekend){
  
  # drop missing values
  weekend <- weekend[!is.na(sleep_duration_per_night)]
  sleep_duration_per_night <- sleep_duration_per_night[!is.na(sleep_duration_per_night)]
  
  if(sum(!weekend) == 0 | sum(weekend) == 0){
    return(NA)
  }
  
  weighted <- 5/7*(!weekend)*sleep_duration_per_night + 2/7*weekend*sleep_duration_per_night
  if(all(is.na(weighted))){
    return(NA)
  }
  
  weighted_sum <- sum(weighted, na.rm = TRUE)
  
  weighted_mean_sleep <- weighted_sum/sum(5/7*(!weekend) + 2/7*weekend)
  
  return(weighted_mean_sleep)
}

#' restructures df such that for each participant one row exists (T1 and T2 beside)
join_Ts <- function(df_aggregated, colnames_to_extract){
  df_aggregated_splitted_at_T <- split(df_aggregated, df_aggregated$MeasurementTime)
  
  df_aggregated_splitted_at_T <- lapply(df_aggregated_splitted_at_T, function(x) {
    index_of_colnames_to_extract <- which(colnames(x) %in% colnames_to_extract)
    names(x)[!names(x) == "Participant"] <- paste(names(x)[!names(x) == "Participant"], unique(x$MeasurementTime),  sep = ".")
    return(x[, index_of_colnames_to_extract])
  })
  
  df_final <- full_join(df_aggregated_splitted_at_T[[1]], df_aggregated_splitted_at_T[[2]], by = "Participant")
  return(df_final)
}