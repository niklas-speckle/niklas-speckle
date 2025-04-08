#' @title Build and merge Data Frames
#' 
#' @description
#' The script builds the sensor and psqi data and merges them into one data frame according to maternal id.
#' 
#' @param 
#' Set the data frames you would like to merge in the config section below with TRUE/FALSE
#' 
#' @return saves the merged data frame to "data/" as `df.rds`

rm(list = ls())
source("psqi-preprocessing/psqi_preprocessing.R", chdir = TRUE)
source("sensor-preprocessing/build_data_frame.R", chdir = TRUE)
rm(list = ls())

#### config section ############################################################

# Set data frames you would like to merge
include_sensor = TRUE
include_psqi = TRUE
include_TL = TRUE
include_covariates = TRUE
# There were additional data frames in the original work

# name of the data frame files
# has to match the name of the sources (`psqi_preprocessing.R` and `build_data_frame.R`) files from above
psqi_filename <- "psqi_index.rds"
sensor_filename <- "df_final.rds"
tl_filename <- "TL.csv"
cov_filename <- "covariates.csv"

################################################################################

df_list <- list()

if(include_sensor){
  load(file.path("../data/", sensor_filename))
  df_sensor <- df
  colnames(df_sensor)[colnames(df_sensor) == "Participant"] <- "id"
  df_list <- append(df_list, list(df_sensor))
}

if(include_psqi){
  load(file.path("../data/", psqi_filename))
  df_psqi <- psqi_index_final
  df_list <- append(df_list, list(df_psqi))
}

if(include_TL){
  df_tl <- read.csv(file.path("../data/", tl_filename))
  df_tl$id <- df_tl$maternal_id
  df_tl <- df_tl[,-grep("maternal_id", colnames(df_tl))]
  df_list <- append(df_list, list(df_tl))
}

if(include_covariates){
  df_cov <- read.csv(file.path("../data/", cov_filename))
  df_list <- append(df_list, list(df_cov))
}


###### join data frames
if(length(df_list) == 0){
  stop("No data frames to merge.")
} else {
  # take first df as base to which other dfs are joined
  df_init <- df_list[[1]]
  df_list <- df_list[-1]
}

# join df by id
for(df in df_list){
  df_init <- full_join(df_init, df, by = "id")
}


# styling
df <- df_init

ordered_cols.T1 <- colnames(df)[grepl("*\\.T1", colnames(df))]
ordered_cols.T2 <- colnames(df)[grepl("*\\.T2", colnames(df))]

ordered_cols <- c("id", ordered_cols.T1, ordered_cols.T2)
ordered_cols <- c(ordered_cols, setdiff(colnames(df), ordered_cols))

df <- df[,c(ordered_cols)]

save(df, file = file.path("../data/", "df.rds"))