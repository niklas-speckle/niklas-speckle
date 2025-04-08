#' @title Builds sensor data frame
#' 
#' @description This script builds the sensor data frame (including sleep, movement, heartrate and some EMA questions)
#' 
#' @note requires all single sensor files in the folder given by `data_src_dir_sensor` in `config.R`
#' 
#' @return The final data frame aggregated over T
#' Additionally, files are saved after each source file. See the sourced files headers.

rm(list = ls())

source("sensor_preprocessing.R")
source("styling.R")



