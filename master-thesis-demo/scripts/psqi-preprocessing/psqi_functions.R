library(dplyr)

#' gets the basename of a file without extension
#' 
#' @param filename: the filename with extension
#' 
#' @return: the basename of the file without extension
get_basename <- function(filename){
  basename <- strsplit(filename, "\\.")[[1]][1]
  return(basename)
}


## data preperation
replace_empty_strings_with_NA <- function(x){
  index_of_empty_strings <- which(x == "")
  x[index_of_empty_strings] <- NA
  return(x)
}


## functions to code and calculate psqi scores according to the manual


calculate_psqi_durat <- function(df){
  
  q4 <- df[[4]]
  rval <- sapply(q4, function(x) {ifelse(x >= 7, 0, 
                                         ifelse(x < 7 & x >= 6, 1,
                                                ifelse(x < 6 & x >= 5, 2, 3)))}
  )
  return(rval)
}

calculate_psqi_distb <- function(df) {
  q5 <- df[,6:14]
  for(i in seq_along(q5[,ncol(q5)])){
    if(is.na(q5[i, ncol(q5)])){
      q5[i, ncol(q5)] <- 0
    }
  }
  q5_sum <- rowSums(q5[,1:ncol(q5)])
  rval <- sapply(q5_sum, function(x) {ifelse(x == 0, 0,
                                             ifelse(x >= 1 & x <= 9, 1,
                                                    ifelse(x > 9 & x <= 18, 2, 3)))
  })
  return(rval)
}

calculate_psqi_laten <- function(df){
  q2_new <- sapply(df[[2]], function(x) {ifelse(x >= 0 & x <= 15, 0,
                                                ifelse(x > 15 & x <= 30, 1,
                                                       ifelse(x > 30 & x <= 60, 2, 3)))})
  
  rval <- sapply(df[[5]]+q2_new, function(x){ifelse(x == 0, 0,
                                                    ifelse(x >= 1 & x <= 2, 1,
                                                           ifelse(x >= 3 & x <= 4, 2, 
                                                                  ifelse(x >= 5 & x <= 6, 3, NA))))})
  return(rval)
}

calculate_psqi_daydys <- function(df){
  rval <- sapply(df[[18]]+df[[19]], function(x){ifelse(x == 0, 0,
                                                       ifelse(x >= 1 & x <= 2, 1,
                                                              ifelse(x >= 3 & x <= 4, 2, 
                                                                     ifelse(x >= 5 & x <= 6, 3, NA))))})
  return(rval)
}


calculate_psqi_hse <- function(df){
  
  diffsec = difftime(df[[3]], df[[1]], units = "secs")
  diffhour = abs(diffsec/3600)
  
  newtib = sapply(diffhour, function(x){ifelse(x > 24, x-24, x)})
  
  tmphse = df[[4]]/newtib*100
  
  rval = sapply(tmphse, function(x){ifelse(x >= 85, 0,
                                           ifelse(x < 85 & x >= 75, 1,
                                                  ifelse(x < 75 & x >= 65, 2, 
                                                         ifelse(x < 65, 3, NA))))})
  
  return(rval)
}


# column order must be correct! First column should be q1
calculate_psqi_index <- function(df){
  durat <- calculate_psqi_durat(df)
  
  if(any(!durat %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("Duration index is out of bounds")
  }
  
  distb <- calculate_psqi_distb(df)
  
  if(any(!distb %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("Disturbances index is out of bounds")
  }
  
  laten <- calculate_psqi_laten(df)
  
  if(any(!laten %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("Latency index is out of bounds")
  }
  
  daydys <- calculate_psqi_daydys(df)
  
  if(any(!daydys %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("Daytime dysfunction index is out of bounds")
  }
  
  hse <- calculate_psqi_hse(df)
  
  if(any(!hse %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("HSE index is out of bounds")
  }
  
  slpqual <- df[[16]]
  
  if(any(!slpqual %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("Sleep quality index is out of bounds")
  }
  
  meds <- df[[17]]
  
  if(any(!meds %in% c(0,1,2,3, NA), na.rm = TRUE)){
    stop("Medication index is out of bounds")
  }
  
  psqi <- durat + distb + laten + daydys + hse + slpqual + meds
  
  if(any(psqi > 21 | psqi < 0, na.rm = TRUE)){
    stop("PSQI index is out of bounds")
  }
  
  rval <- list(
    "sleep_duration_psqi" = df[[4]],
    "durat" = ordered(durat),
    "distb" = ordered(distb),
    "laten" = ordered(laten),
    "daydys" = ordered(daydys),
    "hse" = ordered(hse),
    "slpqual" = ordered(slpqual),
    "meds" = ordered(meds),
    "psqi" = psqi
  )
  
  return(rval)
}