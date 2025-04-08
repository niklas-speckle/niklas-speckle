library(dplyr)


#' Set rows with more than `threshold` missing proportion to NA
#' 
#' @param variable column to check for missing value proportion
#' @param observations vector giving the number of observations from aggregation
#' @param missing vector giving the number of missing values from aggregation
#' @param threshold threshold for missing value proportion 
#' 
set_missing_proportion_to_NA <- function(variable, observations, missing, threshold = THRESHOLD_MISSING_PROPORTION){
  entries_with_enoug_data <- missing/observations < threshold
  variable[!entries_with_enoug_data] <- NA
  return(variable)
}

#' Set unrealistic values to NA
#' 
#' @param variable column to check for unrealistic values
#' @param lower_threshold values lower than threshold are dropped
#' @param upper_threshold values higher than threshold are dropped
drop_unrealistic <- function(variable, lower_threshold, upper_threshold){
  realistic_idx <- which(between(variable, lower_threshold, upper_threshold))
  variable[-realistic_idx] <- NA
  return(variable)
}