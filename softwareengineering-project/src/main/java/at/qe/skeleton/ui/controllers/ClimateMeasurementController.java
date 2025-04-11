package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.ClimateMeasurement;
import at.qe.skeleton.model.Limits;
import at.qe.skeleton.model.SensorType;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.climate.ClimateMeasurementService;
import at.qe.skeleton.services.room.LimitService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Controller for climate measurements.
 */

@Getter
@Setter
@Component
@Scope("view")
public class ClimateMeasurementController implements Serializable {

    @Autowired
    private transient ClimateMeasurementService climateMeasurementService;

    @Autowired
    private transient LimitService limitService;

    @Autowired
    private SessionInfoBean sessionInfoBean;


    private List<ClimateMeasurement> allMeasurementsForList;

    @PostConstruct
    public void init() {
        getAllMeasurements();
    }


    /**
     * Returns a list of all measurements.
     * Only for developing.
     */
    public List<ClimateMeasurement> getAllMeasurementsTest() {
        return climateMeasurementService.findAllMeasurements();
    }

    /**
     * Returns a list of all measurements for the current user.
     */
    public List<ClimateMeasurement> getAllMeasurements() {
        allMeasurementsForList = climateMeasurementService.findAllMeasurementsByUser(sessionInfoBean.getCurrentUser());
        return allMeasurementsForList;
    }


    /**
     * Retrieves the current temperature for the current user as formatted String.
     *
     * Returns the latest temperature measurement from current user's TemperaDevice as formatted String
     * or a message that no current temperature measurement was found.
     *
     * @return formatted temperature measurement or message
     */
    public String getTemperature() {
        try {
            Double currentTemperature = climateMeasurementService.findCurrentMeasurementForUser(sessionInfoBean.getCurrentUser(), SensorType.AIR_TEMPERATURE);
            return currentTemperature.toString()+" C°";
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    /**
     * Retrieves the current humidity for the current user as formatted String.
     *
     * Returns the latest humidity measurement from current user's TemperaDevice as formatted String
     * or a message that no current humidity measurement was found.
     *
     * @return formatted humidity measurement or message
     */
    public String getHumidity() {
        try {
            Double currentTemperature = climateMeasurementService.findCurrentMeasurementForUser(sessionInfoBean.getCurrentUser(), SensorType.AIR_HUMIDITY);
            return currentTemperature.toString()+" %";
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    /**
     * Retrieves the current air quality for the current user as formatted String.
     *
     * Returns the latest air quality measurement from current user's TemperaDevice as formatted String
     * or a message that no current air quality measurement was found.
     *
     * @return formatted air quality measurement or message
     */
    public String getAirQuality() {
        try {
            Double currentTemperature = climateMeasurementService.findCurrentMeasurementForUser(sessionInfoBean.getCurrentUser(), SensorType.AIR_QUALITY);
            return currentTemperature.toString()+" ppm";
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    /**
     * Retrieves the current light for the current user as formatted String.
     *
     * Returns the latest light measurement from current user's TemperaDevice as formatted String
     * or a message that no current light measurement was found.
     *
     * @return formatted light measurement or message
     */
    public String getLight() {
        try {
            Double currentTemperature = climateMeasurementService.findCurrentMeasurementForUser(sessionInfoBean.getCurrentUser(), SensorType.LIGHT_INTENSITY);
            return currentTemperature.toString()+" lux";
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    /**
     * Retrieves a color based on the current measurement for temperature.
     * @return The hex code of the color as String.
     */
    public String getTemperatureColor() {
        return getColorForMeasurement(SensorType.AIR_TEMPERATURE);
    }

    /**
     * Retrieves a color based on the current measurement for humidity.
     * @return The hex code of the color as String.
     */
    public String getHumidityColor() {
        return getColorForMeasurement(SensorType.AIR_HUMIDITY);
    }

    /**
     * Retrieves a color based on the current measurement for air quality.
     * @return The hex code of the color as String.
     */
    public String getAirQualityColor() {
        return getColorForMeasurement(SensorType.AIR_QUALITY);
    }

    /**
     * Retrieves a color based on the current measurement for light.
     * @return The hex code of the color as String.
     */
    public String getLightColor() {
        return getColorForMeasurement(SensorType.LIGHT_INTENSITY);
    }


    public String getTemperatureMessageUpper() {
        try {
            Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.AIR_TEMPERATURE);
            return limits.getMessageUpper();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }
    public String getTemperatureMessageLower() {
        try {
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.AIR_TEMPERATURE);
        return limits.getMessageLower();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    public String getHumidityMessageUpper() {
        try {
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.AIR_HUMIDITY);
        return limits.getMessageUpper();
    } catch (NoSuchElementException e) {
        return e.getMessage();
    }
    }
    public String getHumidityMessageLower() {
        try {
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.AIR_HUMIDITY);
        return limits.getMessageLower();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    public String getAirQualityMessageUpper() {
        try {
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.AIR_QUALITY);
        return limits.getMessageUpper();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }
    public String getAirQualityMessageLower() {
        try {
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.AIR_QUALITY);
        return limits.getMessageLower();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }

    public String getLightMessageUpper() {
        try{
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.LIGHT_INTENSITY);
        return limits.getMessageUpper();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }
    public String getLightMessageLower() {
        try{
        Limits limits = limitService.getLimitsForUserAndSensorType(sessionInfoBean.getCurrentUser(), SensorType.LIGHT_INTENSITY);
        return limits.getMessageLower();
        } catch (NoSuchElementException e) {
            return e.getMessage();
        }
    }
    /**
     * Retrieves a color based on the current measurement for the specified sensor type.
     * If the measurement is within the defined limits, it returns a color indicating the status (green for OK, orange for close to threshold, above or below limit).
     * If no threshold values are configured for the user or if an error occurs during retrieval, a default grey color is returned.
     *
     * @param sensorType The type of sensor for which to retrieve the background color.
     * @return The background color corresponding to the current measurement status.
     */
    private String getColorForMeasurement(SensorType sensorType) {
        try {
            Userx currentUser = sessionInfoBean.getCurrentUser();
            Double currentMeasurement = climateMeasurementService.findCurrentMeasurementForUser(sessionInfoBean.getCurrentUser(), sensorType);
            Limits limits = limitService.getLimitsForUserAndSensorType(currentUser, sensorType);
            Double upperLimit = limits.getUpperLimit();
            Double lowerLimit = limits.getLowerLimit();
            if ((currentMeasurement < lowerLimit) || (currentMeasurement > upperLimit)) {
                return "#fd7b83"; // red
            } else if (isCloseToLimit(currentMeasurement, upperLimit, lowerLimit, sensorType)) {
                return "#ffd184"; // orange
            } else {
                return "#92e38a"; // green
            }
        } catch (NoSuchElementException e) {
            // If no threshold values are configured for the user, display an info message and return a default grey color, but not in the welcome page.
            String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
            if (!"/secured/welcome.xhtml".equals(viewId)) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "No temperature threshold values are configured for your room."));
            }
            return "#dadada"; // grey
        }
    }

    /**
     * Checks if the current measurement is close to the upper or lower limit for the specified sensor type.
     * The threshold for being close to the limit is defined based on the sensor type.
     *
     * @param measuredValue The current measurement value.
     * @param upperLimit The upper limit for the sensor.
     * @param lowerLimit The lower limit for the sensor.
     * @param sensorType The type of sensor.
     * @return True if the measurement is close to the limit, otherwise false.
     */
    private boolean isCloseToLimit(double measuredValue, Double upperLimit, Double lowerLimit, SensorType sensorType) {
        switch (sensorType) {
            case AIR_TEMPERATURE -> {return (measuredValue < lowerLimit+2 || measuredValue > upperLimit-2);}
            case AIR_HUMIDITY -> {return (measuredValue < lowerLimit+5 || measuredValue > upperLimit-5);}
            case AIR_QUALITY -> {return (measuredValue < lowerLimit+5 || measuredValue > upperLimit-5);}
            default -> {return (measuredValue < lowerLimit+50 || measuredValue > upperLimit-50);}
        }
    }


}
