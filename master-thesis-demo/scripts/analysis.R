# Primary hypotheses:
# -	H1: Maternal sleep duration measured by actigraphy has an inverted U-shaped relationship with newborn telomere length. 
# -	H2: Maternal sleep duration measured by PSQI has an inverted U-shaped relationship with newborn telomere length. 
# -	H3: Maternal sleep duration measured by sleep diary has an inverted U-shaped relationship with newborn telomere length. 
# -	H4: Maternal global PSQI score is negatively correlated with newborn telomere length.
# - H5: Diary Sleep Quality is negatively correlated with newborn telomere length.
################################################################################

source("build_data_frame.R")
rm(list = ls())

################################################################################

library("psych")
library("pwr")
library("ggplot2")
library("ggpubr")
library("coin")


#' Tests some assumptions for linear regression
lm_diagnostics <- function(model){
  plot(model, which = 1)
  
  # assumption 1: normality of Residuals
  shapiro_results <- shapiro.test(rstandard(model))
  if(shapiro_results$p.value < 0.05){
    print("Residuals are not normally distributed")
  }
  # assumption 2: homoskedasticity
  ncv_results <- ncvTest(model)
  if(ncv_results$p < 0.05){
    print("Residuals are not homoskedastic")
  }
  # assumption 3: multicollinearity
  if(length(attr(model$terms, "term.labels")) >= 2){
    vif_results <- vif(model)
    
    if(is.null(dim(vif_results))){
      if(any(vif_results > 1.5)){
        print("Multicollinearity is present")
        print(vif_results)
      }
    } else if(any(vif_results[,1] > 1.5)){
      print("Multicollinearity is present")
      print(vif_results)
    }
  }
  
}

#' creates new lm model with covariates added
add_covariates <- function(model, covariates, data = df){
  formula <- as.formula(paste(deparse(formula(model)), paste(covariates, collapse = " + "), sep = " + "))
  return(lm(formula, data = data))
}


load("../data/df.rds")
covariates <- c("maternal_age", "child_sex", "gestational_week", "SES", "BMI")

################################################################################

# categorize sleep variables in long and short according to theory
df$sensor_sleep_duration.T1_cut <- 
  cut(df$sensor_sleep_duration.T1, breaks=c(-Inf, 7, 9, Inf), labels=c("short","normal","long"))
df$sensor_sleep_duration.T2_cut <- 
  cut(df$sensor_sleep_duration.T2, breaks=c(-Inf, 7, 9, Inf), labels=c("short","normal","long"))
df$psqi_4_hourssleep.T1_cut <-
  cut(df$psqi_4_hourssleep.T1, breaks=c(-Inf, 7, 9, Inf), labels=c("short","normal","long"))
df$psqi_4_hourssleep.T2_cut <-
  cut(df$psqi_4_hourssleep.T2, breaks=c(-Inf, 7, 9, Inf), labels=c("short","normal","long"))
df$sleep_04_durat.T1_cut <-
  cut(df$sleep_04_durat.T1, breaks=c(-Inf, 7, 9, Inf), labels=c("short","normal","long"))
df$sleep_04_durat.T2_cut <-
  cut(df$sleep_04_durat.T2, breaks=c(-Inf, 7, 9, Inf), labels=c("short","normal","long"))
df$sleep_01_quality.T1 <- as.factor(df$sleep_01_quality.T1)                                       
df$sleep_01_quality.T2 <- as.factor(df$sleep_01_quality.T2)


describe(df[c("TSratio",
              "sensor_sleep_duration.T1", 
              "sensor_sleep_duration.T2", 
              "psqi.T1", 
              "psqi.T2", 
              "sleep_01_quality.T1",
              "sleep_01_quality.T2",
              "psqi_4_hourssleep.T1",
              "psqi_4_hourssleep.T2",
              "sleep_04_durat.T1",
              "sleep_04_durat.T2")])


##################### power analysis ###########################################

# aim for medium effect size in f2 pwr test
effect_size_f2 <- 0.15 # medium effect size

# H1:  Maternal sleep duration has an inverted U-shaped relation with newborn telomere length

# H1.1.: Actigraphy sleep duration
# T1
n_obs <- sum(complete.cases(df[,c("sensor_sleep_duration.T1", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 3 # intercept + 1 predictors of poly(2) = 3 predictors
degrees_of_freedom_numerator <- 3 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/6) # with inverted U shape (poly(3))
# T2
n_obs <- sum(complete.cases(df[,c("sensor_sleep_duration.T2", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 3 # intercept + 1 predictors of poly(2) = 3 predictors
degrees_of_freedom_numerator <- 3 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/6) # with inverted U shape (poly(3))

# H1.2.: PSQI sleep duration
# T1
n_obs <- sum(complete.cases(df[,c("psqi_4_hourssleep.T1", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 3 # intercept + 1 predictors of poly(2) = 3 predictors
degrees_of_freedom_numerator <- 3 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/6) # with inverted U shape (poly(3))
# T2
n_obs <- sum(complete.cases(df[,c("psqi_4_hourssleep.T2", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 3 # intercept + 1 predictors of poly(2) = 3 predictors
degrees_of_freedom_numerator <- 3 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/6) # with inverted U shape (poly(3))

# H1.3.: Sleep diary sleep duration
# T1
n_obs <- sum(complete.cases(df[,c("sleep_04_durat.T1", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 3 # intercept + 1 predictors of poly(2) = 3 predictors
degrees_of_freedom_numerator <- 3 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/6) # with inverted U shape (poly(3))
# T2
n_obs <- sum(complete.cases(df[,c("sleep_04_durat.T2", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 3 # intercept + 1 predictors of poly(2) = 3 predictors
degrees_of_freedom_numerator <- 3 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/6) # with inverted U shape (poly(3))

# H2: Maternal sleep quality has a positive association with newborn telomere length
# H2.1.: PSQI global score
# T1
n_obs <- sum(complete.cases(df[,c("psqi.T1", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 2 # intercept + 1 predictor = 2 predictors
degrees_of_freedom_numerator <- 2 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/4)
# T2
n_obs <- sum(complete.cases(df[,c("psqi.T2", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 2 # intercept + 1 predictor = 2 predictors
degrees_of_freedom_numerator <- 2 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/4)

# H2.2. sleep diary sleep quality
# T1
n_obs <- sum(complete.cases(df[,c("sleep_01_quality.T1", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 2 # intercept + 1 predictor = 2 predictors
degrees_of_freedom_numerator <- 2 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/4)
# T2
n_obs <- sum(complete.cases(df[,c("sleep_01_quality.T2", "TSratio")]))
degrees_of_freedom_denominator <- n_obs - 2 # intercept + 1 predictor = 2 predictors
degrees_of_freedom_numerator <- 2 - 1
pwr.f2.test(degrees_of_freedom_numerator, degrees_of_freedom_denominator, effect_size_f2, sig.level = 0.05/4)



##################### do sleep measures differ? ################################

# inspect normality of differences for paired t-test. As sample size is large enough t-test can be used without perfect normality
# the difference in the general population can be assumed to be normally distributed
plot(density(df$sensor_sleep_duration.T1 - df$psqi_4_hourssleep.T1, na.rm = TRUE))
plot(density(df$sensor_sleep_duration.T1 - df$sleep_04_durat.T1, na.rm = TRUE))
plot(density(df$psqi_4_hourssleep.T1 - df$sleep_04_durat.T1, na.rm = TRUE))
plot(density(df$sensor_sleep_duration.T2 - df$psqi_4_hourssleep.T2, na.rm = TRUE))
plot(density(df$sensor_sleep_duration.T2 - df$sleep_04_durat.T2, na.rm = TRUE))
plot(density(df$psqi_4_hourssleep.T2 - df$sleep_04_durat.T2, na.rm = TRUE))

# T1
t.test(df$sensor_sleep_duration.T1, df$psqi_4_hourssleep.T1, paired = TRUE)
t.test(df$sensor_sleep_duration.T1, df$sleep_04_durat.T1, paired = TRUE)
t.test(df$psqi_4_hourssleep.T1, df$sleep_04_durat.T1, paired = TRUE)
# T2
t.test(df$sensor_sleep_duration.T2, df$psqi_4_hourssleep.T2, paired = TRUE)
t.test(df$sensor_sleep_duration.T2, df$sleep_04_durat.T2, paired = TRUE)
t.test(df$psqi_4_hourssleep.T2, df$sleep_04_durat.T2, paired = TRUE)

################### do sleep durations decrease? ###############################

plot(density(df$sensor_sleep_duration.T1 - df$sensor_sleep_duration.T2, na.rm = TRUE))
plot(density(df$psqi_4_hourssleep.T1 - df$psqi_4_hourssleep.T2, na.rm = TRUE))
plot(density(df$sleep_04_durat.T1 - df$sleep_04_durat.T2, na.rm = TRUE))
t.test(df$sensor_sleep_duration.T1, df$sensor_sleep_duration.T2, paired = TRUE)
t.test(df$psqi_4_hourssleep.T1, df$psqi_4_hourssleep.T2, paired = TRUE)
t.test(df$sleep_04_durat.T1, df$sleep_04_durat.T2, paired = TRUE)

plot(density(df$psqi.T1 - df$psqi.T2, na.rm = TRUE))
t.test(df$psqi.T1, df$psqi.T2, paired = TRUE)

fisher.test(df$sleep_01_quality.T1, df$sleep_01_quality.T2, simulate.p.value = TRUE)

################### PLOT #######################################################

######## sleep duration
ggp_sensor_T1 <- ggplot(df, aes(sensor_sleep_duration.T1, TSratio) ) + 
  geom_point() +
  xlim(4, 11.5) +
  geom_smooth(formula = y ~ poly(x, degree = 2), method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Actigraphy Sleep Duration (hours) T1")

ggp_sensor_T2 <- ggplot(df, aes(sensor_sleep_duration.T2, TSratio) ) + 
  geom_point() +
  xlim(4, 10) +
  geom_smooth(formula = y ~ poly(x, degree = 2), method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Actigraphy Sleep Duration (hours) T2")

ggp_psqi_sleep_durat_T1 <- ggplot(df, aes(psqi_4_hourssleep.T1, TSratio)) + 
  geom_point() +
  xlim(5, 10) +
  geom_smooth(formula = y ~ poly(x, degree = 2), method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Sleep Duration (hours) T1")

ggp_psqi_sleep_durat_T2 <- ggplot(df, aes(psqi_4_hourssleep.T2, TSratio)) + 
  geom_point() +
  xlim(4, 10) +
  geom_smooth(formula = y ~ poly(x, degree = 2), method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Sleep Duration (hours) T2")

ggp_sleep_diary_sleep_durat_T1 <- ggplot(df, aes(sleep_04_durat.T1, TSratio)) +
  geom_point() +
  xlim(4.5, 9.5) +
  geom_smooth(formula = y ~ poly(x, degree = 2), method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Duration (hours) T1")

ggp_sleep_diary_sleep_durat_T2 <- ggplot(df, aes(sleep_04_durat.T2, TSratio)) +
  geom_point() +
  xlim(4, 9.5) +
  geom_smooth(formula = y ~ poly(x, degree = 2), method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Duration (hours) T2")

ggarrange(ggp_sensor_T1, ggp_sensor_T2, 
          ggp_psqi_sleep_durat_T1, ggp_psqi_sleep_durat_T2, 
          ggp_sleep_diary_sleep_durat_T1, ggp_sleep_diary_sleep_durat_T2,
          ncol = 2, nrow = 3)


######### sleep quality

ggp_PSQI_T1 <- ggplot(df, aes(psqi.T1, TSratio)) + 
  geom_point() +
  xlim(0, 14) +
  geom_smooth(formula = y ~ x, method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Score T1")

ggp_PSQI_T2 <- ggplot(df, aes(psqi.T2, TSratio)) +
  geom_point() +
  xlim(0, 14) +
  geom_smooth(formula = y ~ x, method = "lm") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Score T2")

ggp_diary_quality_T1 <- ggplot(df[df$sleep_01_quality.T1 %in% c(1,1.5,2,2.5,3),], aes(sleep_01_quality.T1, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Quality T1")

ggp_diary_quality_T2 <- ggplot(df[!is.na(df$sleep_01_quality.T2),], aes(sleep_01_quality.T2, TSratio)) +
  geom_boxplot(outlier.colour = "black") + 
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Quality T2")

ggarrange(ggp_PSQI_T1, ggp_PSQI_T2, ggp_diary_quality_T1, ggp_diary_quality_T2, ncol = 2, nrow = 2)


###################### plots for robust test statistics (see below)

######### sleep durations

ggp_box_sensor_T1 <- ggplot(df[!is.na(df$sensor_sleep_duration.T1_cut),], aes(sensor_sleep_duration.T1_cut, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Actigraphy Sleep Duration T1")

ggp_box_sensor_T2 <- ggplot(df[!is.na(df$sensor_sleep_duration.T2_cut),], aes(sensor_sleep_duration.T2_cut, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Actigraphy Sleep Duration T2")

ggp_box_psqi_T1 <- ggplot(df[!is.na(df$psqi_4_hourssleep.T1_cut),], aes(psqi_4_hourssleep.T1_cut, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Sleep Duration T1")

ggp_box_psqi_T2 <- ggplot(df[!is.na(df$psqi_4_hourssleep.T2_cut),], aes(psqi_4_hourssleep.T2_cut, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Sleep Duration T2")

ggp_box_diary_T1 <- ggplot(df[!is.na(df$sleep_04_durat.T1_cut),], aes(sleep_04_durat.T1_cut, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Duration T1")

ggp_box_diary_T2 <- ggplot(df[!is.na(df$sleep_04_durat.T2_cut),], aes(sleep_04_durat.T2_cut, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Duration T2")

ggarrange(ggp_box_sensor_T1, ggp_box_sensor_T2, ggp_box_psqi_T1, ggp_box_psqi_T2, ggp_box_diary_T1, ggp_box_diary_T2, ncol = 2, nrow = 3)

######### sleep quality

ggp_box_psqi_quality_T1 <- ggplot(df[!is.na(df$psqi_is_good_sleep_quality.T1),], aes(psqi_is_good_sleep_quality.T1, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Sleep Quality T1")

ggp_box_psqi_quality_T2 <- ggplot(df[!is.na(df$psqi_is_good_sleep_quality.T2),], aes(psqi_is_good_sleep_quality.T2, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("PSQI Sleep Quality T2")

ggp_box_diary_quality_T1 <- ggplot(df[!is.na(df$sleep_01_quality.T1),], aes(sleep_01_quality.T1, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Quality T1")

ggp_box_diary_quality_T2 <- ggplot(df[!is.na(df$sleep_01_quality.T2),], aes(sleep_01_quality.T2, TSratio)) +
  geom_boxplot(outlier.colour = "black") +
  ylab("Telomere Length (T/S Ratio)") +
  xlab("Sleep Diary Sleep Quality T2")

ggarrange(ggp_box_psqi_quality_T1, ggp_box_psqi_quality_T2, ggp_diary_quality_T1, ggp_diary_quality_T2, ncol = 2, nrow = 2)

################### Main Analysis ##############################################

# H1 Maternal sleep duration measured by actigraphy has an inverted U-shaped relationship with newborn telomere length. 
lm_H1_T1 <- lm(TSratio ~ poly(sensor_sleep_duration.T1, 2, raw = TRUE), data = df)
summary(lm_H1_T1)
lm_diagnostics(lm_H1_T1)

lm_H1_T2 <- lm(TSratio ~ poly(sensor_sleep_duration.T2, 2, raw = TRUE), data = df)
summary(lm_H1_T2)
lm_diagnostics(lm_H1_T2)

# H2 Maternal sleep duration measured by PSQI has an inverted U-shaped relationship with newborn telomere length. 
lm_H2_T1 <- lm(TSratio ~ poly(psqi_4_hourssleep.T1, 2, raw = TRUE), data = df)
summary(lm_H2_T1)
lm_diagnostics(lm_H2_T1)

lm_H2_T2 <- lm(TSratio ~ poly(psqi_4_hourssleep.T2, 2, raw = TRUE), data = df)
summary(lm_H2_T2)
lm_diagnostics(lm_H2_T2)

# H3 Maternal sleep duration measured by sleep diary has an inverted U-shaped relationship with newborn telomere length. 
lm_H3_T1 <- lm(TSratio ~ poly(sleep_04_durat.T1, 2, raw = TRUE), data = df)
summary(lm_H3_T1)
lm_diagnostics(lm_H3_T1)

lm_H3_T2 <- lm(TSratio ~ poly(sleep_04_durat.T2, 2, raw = TRUE), data = df)
summary(lm_H3_T2)
lm_diagnostics(lm_H3_T2)

# H4 Maternal global PSQI score is negatively correlated with newborn telomere length. 
lm_H4_T1 <- lm(TSratio ~ psqi.T1, data = df)
summary(lm_H4_T1)
lm_diagnostics(lm_H4_T1)

lm_H4_T2 <- lm(TSratio ~ psqi.T2, data = df)
summary(lm_H4_T2)
lm_diagnostics(lm_H4_T2)

# H5 Diary Sleep Quality is negatively correlated with newborn telomere length.
lm_H5_T1 <- lm(TSratio ~ sleep_01_quality.T1, data = df)
summary(lm_H5_T1)
lm_diagnostics(lm_H5_T1)

lm_H5_T2 <- lm(TSratio ~ sleep_01_quality.T2, data = df)
summary(lm_H5_T2)
lm_diagnostics(lm_H5_T2)

######### add covariates

# H1 Maternal sleep duration measured by actigraphy has an inverted U-shaped relationship with newborn telomere length. 
lm_H1_T1_cov <- add_covariates(lm_H1_T1, covariates)
summary(lm_H1_T1_cov)
lm_diagnostics(lm_H1_T1_cov)

lm_H1_T2_cov <- add_covariates(lm_H1_T2, covariates)
summary(lm_H1_T2_cov)
lm_diagnostics(lm_H1_T2_cov)

# H2 Maternal sleep duration measured by PSQI has an inverted U-shaped relationship with newborn telomere length. 
lm_H2_T1_cov <- add_covariates(lm_H2_T1, covariates)
summary(lm_H2_T1_cov)
lm_diagnostics(lm_H2_T1_cov)

lm_H2_T2_cov <- add_covariates(lm_H2_T2, covariates)
summary(lm_H2_T2_cov)
lm_diagnostics(lm_H2_T2_cov)

# H3 Maternal sleep duration measured by sleep diary has an inverted U-shaped relationship with newborn telomere length. 
lm_H3_T1_cov <- add_covariates(lm_H3_T1, covariates)
summary(lm_H3_T1_cov)
lm_diagnostics(lm_H3_T1_cov)

lm_H3_T2_cov <- add_covariates(lm_H3_T2, covariates)
summary(lm_H3_T2_cov)
lm_diagnostics(lm_H3_T2_cov)

# H4 Maternal global PSQI score is negatively correlated with newborn telomere length.
lm_H4_T1_cov <- add_covariates(lm_H4_T1, covariates)
summary(lm_H4_T1_cov)
lm_diagnostics(lm_H4_T1_cov)

lm_H4_T2_cov <- add_covariates(lm_H4_T2, covariates)
summary(lm_H4_T2_cov)
lm_diagnostics(lm_H4_T2_cov)

# H5 Diary Sleep Quality is negatively correlated with newborn telomere length.
lm_H5_T1_cov <- add_covariates(lm_H5_T1, covariates)
summary(lm_H5_T1_cov)
lm_diagnostics(lm_H5_T1_cov)

lm_H5_T2_cov <- add_covariates(lm_H5_T2, covariates)
summary(lm_H5_T2_cov)
lm_diagnostics(lm_H5_T2_cov)

######### sensitivity analysis - robust test statistics

median_test(df$TSratio ~ df$sensor_sleep_duration.T1_cut)
median_test(df$TSratio ~ df$sensor_sleep_duration.T2_cut)
median_test(df$TSratio ~ df$psqi_4_hourssleep.T1_cut)
aggregate(df$TSratio, list(df$psqi_4_hourssleep.T1_cut), median, na.rm = TRUE)
median_test(df$TSratio ~ df$psqi_4_hourssleep.T2_cut)
aggregate(df$TSratio, list(df$psqi_4_hourssleep.T2_cut), median, na.rm = TRUE)
median_test(df$TSratio ~ df$sleep_04_durat.T1_cut)
median_test(df$TSratio ~ df$sleep_04_durat.T2_cut)

median_test(df$TSratio ~ df$psqi_is_good_sleep_quality.T1)
aggregate(df$TSratio, list(df$psqi_is_good_sleep_quality.T1), median, na.rm = TRUE)
median_test(df$TSratio ~ df$psqi_is_good_sleep_quality.T2)
aggregate(df$TSratio, list(df$psqi_is_good_sleep_quality.T2), median, na.rm = TRUE)
median_test(df$TSratio ~ df$sleep_01_quality.T1)
aggregate(df$TSratio, list(df$sleep_01_quality.T1), median, na.rm = TRUE)
median_test(df$TSratio ~ df$sleep_01_quality.T2)








