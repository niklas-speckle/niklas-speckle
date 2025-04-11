package at.qe.skeleton.services.climate;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.ClimateMeasurementRepository;

import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.room.LimitService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service class for managing climate measurements.
 */

@Component
public class ClimateMeasurementService {

    @Autowired
    ClimateMeasurementRepository measurementRepository;

    @Autowired
    TemperaDeviceService temperaDeviceService;

    @Autowired
    LimitService limitService;

    @Autowired
    WarningService warningService;



    @Transactional
    public void save(ClimateMeasurement measurement) {
        measurementRepository.save(measurement);
    }

    @Transactional
    public List<ClimateMeasurement> saveAll(List<ClimateMeasurement> measurements) {
        return measurementRepository.saveAll(measurements);
    }

    @Transactional
    public void delete(ClimateMeasurement measurement) {
        measurementRepository.delete(measurement);
    }

    public List<ClimateMeasurement> findAllMeasurements() {
        return measurementRepository.findAll();
    }

    public Optional<ClimateMeasurement> findMeasurementById(Long id) {
        return measurementRepository.findById(id);
    }

    public List<ClimateMeasurement> findAllMeasurementsByUser(Userx user) {
        List<ClimateMeasurement> measurements = new ArrayList<>();
        if (user.getTemperaDevice() != null) {
            for (Sensor sensor : user.getTemperaDevice().getSensors()) {
                measurements.addAll(measurementRepository.findAllBySensor(sensor));
            }
        }
            return measurements;
    }

    /**
     * Retrieves a list of climate measurements associated with a specific sensor and falling within the specified time range.
     * Measurements are sorted by their timestamp in ascending order.
     * @param sensor The sensor for which to retrieve the measurements.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @return A list of climate measurements associated with the specified sensor and falling within the specified time range.
     */
    public List<ClimateMeasurement> findSensorMeasurementsBetween(Sensor sensor, LocalDate start, LocalDate end) {
        LocalDateTime after = start.atStartOfDay();
        LocalDateTime before = end.plusDays(1).atStartOfDay();
        return measurementRepository.findAllBySensorAndTimeStampAfterAndTimeStampBefore(sensor, after, before);
    }

    /**
     * Retrieves the temperature history for a given user within a specified time range.
     * Measurements are filtered based on the specified granularity in minutes.
     * @param user The user for whom to retrieve the temperature history.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param granularityInMinutes The minimum time difference (granularity) in minutes between consecutive measurements.
     * @return A list of temperature measurements for the user within the specified time range and granularity.
     */
    public List<ClimateMeasurement> temperatureHistoryForUser(Userx user, LocalDate start, LocalDate end, int granularityInMinutes) {
        if (user.getTemperaDevice() == null) {
            return new ArrayList<>();
        } else {
            Sensor temperatureSensor = user.getTemperaDevice().getTemperatureSensor();
            List<ClimateMeasurement> allTemperatureMeasurementsBetween = findSensorMeasurementsBetween(temperatureSensor, start, end);
            return filterMeasurements(allTemperatureMeasurementsBetween, granularityInMinutes);
        }
    }

    /**
     * Retrieves the humidity history for a given user within a specified time range.
     * Measurements are filtered based on the specified granularity in minutes.
     * @param user The user for whom to retrieve the humidity history.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param granularityInMinutes The minimum time difference (granularity) in minutes between consecutive measurements.
     * @return A list of humidity measurements for the user within the specified time range and granularity.
     */
    public List<ClimateMeasurement> humidityHistoryForUser(Userx user, LocalDate start, LocalDate end, int granularityInMinutes) {
        if (user.getTemperaDevice() == null) {
            return new ArrayList<>();
        } else {
            Sensor humiditySensor = user.getTemperaDevice().getHumiditySensor();
            List<ClimateMeasurement> allHumidityMeasurementsBetween = findSensorMeasurementsBetween(humiditySensor, start, end);
            return filterMeasurements(allHumidityMeasurementsBetween, granularityInMinutes);
        }
    }

    /**
     * Retrieves the air quality history for a given user within a specified time range.
     * Measurements are filtered based on the specified granularity in minutes.
     * @param user The user for whom to retrieve the air quality history.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param granularityInMinutes The minimum time difference (granularity) in minutes between consecutive measurements.
     * @return A list of air quality measurements for the user within the specified time range and granularity.
     */
    public List<ClimateMeasurement> airQualityHistoryForUser(Userx user, LocalDate start, LocalDate end, int granularityInMinutes) {
        if (user.getTemperaDevice() == null) {
            return new ArrayList<>();
        } else {
            Sensor airQualitySensor = user.getTemperaDevice().getAirQualitySensor();
            List<ClimateMeasurement> allAirQualityMeasurementsBetween = findSensorMeasurementsBetween(airQualitySensor, start, end);
            return filterMeasurements(allAirQualityMeasurementsBetween, granularityInMinutes);
        }
    }

    /**
     * Retrieves the light history for a given user within a specified time range.
     * Measurements are filtered based on the specified granularity in minutes.
     * @param user The user for whom to retrieve the light history.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param granularityInMinutes The minimum time difference (granularity) in minutes between consecutive measurements.
     * @return A list of light measurements for the user within the specified time range and granularity.
     */
    public List<ClimateMeasurement> lightHistoryForUser(Userx user, LocalDate start, LocalDate end, int granularityInMinutes) {
        if (user.getTemperaDevice() == null) {
            return new ArrayList<>();
        } else {
            Sensor lightSensor = user.getTemperaDevice().getLightSensor();
            List<ClimateMeasurement> allLightMeasurementsBetween = findSensorMeasurementsBetween(lightSensor, start, end);
            return filterMeasurements(allLightMeasurementsBetween, granularityInMinutes);
        }
    }


    /**
     * Retrieves the most recent measurement of a specific type for a given user.
     * Throws a NoSuchElementException if no current measurement is found.
     * @param user The user for whom to retrieve the measurement.
     * @param sensorType The type of sensor for which to retrieve the measurement.
     * @return The value of the most recent measurement for the specified sensor type.
     * @throws NoSuchElementException If no current measurement is found for the specified sensor type.
     */
    public Double findCurrentMeasurementForUser(Userx user, SensorType sensorType) throws NoSuchElementException {
        Sensor sensor = getSensor(user, sensorType);
        ClimateMeasurement currentMeasurement = measurementRepository.findNewestBySensorAndNotOlderThan(sensor.getId(), LocalDateTime.now().minusDays(1))
              .orElseThrow(() -> new NoSuchElementException("No current "+ sensorType.toString().toLowerCase() +" measurement found."));
        return currentMeasurement.getMeasuredValue();
    }

    /**
     * Retrieves the sensor corresponding to the specified sensor type for a given user.
     * Throws a NoSuchElementException if no TemperaDevice is found for the given user.
     * @param user The user for whom to retrieve the sensor.
     * @param sensorType The type of sensor to retrieve.
     * @return The sensor corresponding to the specified sensor type.
     * @throws NoSuchElementException if no TemperaDevice is found for the given user.
     */
    private Sensor getSensor(Userx user, SensorType sensorType) {
        if (user.getTemperaDevice() == null) {
            throw new NoSuchElementException("No TemperaDevice found for user "+ user.getUsername());
        }
        TemperaDevice usersDevice = user.getTemperaDevice();
        Sensor sensor;
        switch (sensorType) {
            case AIR_TEMPERATURE -> sensor = usersDevice.getTemperatureSensor();
            case AIR_HUMIDITY -> sensor = usersDevice.getHumiditySensor();
            case AIR_QUALITY -> sensor = usersDevice.getAirQualitySensor();
            default -> sensor = usersDevice.getLightSensor();
        }
        return sensor;
    }

    /**
     * Filters a list of climate measurements based on the specified granularity in minutes.
     * Returns a list of climate measurements ordered by timestamp in ascending order.
     * @param allMeasurements The list of all measurements to filter.
     * @param granularityInMinutes The minimum time difference (granularity) in minutes between consecutive measurements.
     * @return A filtered list of climate measurements based on the specified granularity.
     */
    private List<ClimateMeasurement> filterMeasurements(List<ClimateMeasurement> allMeasurements, int granularityInMinutes) {
        List<ClimateMeasurement> filteredMeasurements = new ArrayList<>();
        if (!allMeasurements.isEmpty()) {

            filteredMeasurements.add(allMeasurements.get(0)); // add first measurement
            LocalDateTime previousTimeStamp = allMeasurements.get(0).getTimeStamp();

            for (int i = 1; i < allMeasurements.size(); i++) { // iterate over all measurements
                ClimateMeasurement currentMeasurement = allMeasurements.get(i);
                LocalDateTime currentTimeStamp = currentMeasurement.getTimeStamp();
                long timeDifferenceMinutes = Duration.between(previousTimeStamp, currentTimeStamp).toMinutes();
                // timeDifference can be positive or negative --> abs()
                if (Math.abs(timeDifferenceMinutes) >= granularityInMinutes) {
                    filteredMeasurements.add(currentMeasurement);
                    previousTimeStamp = currentTimeStamp;
                }
            }
        }
        return filteredMeasurements;
    }



    /**
     * checks if the values of incoming measurements are within the limits of the respective room where
     * the TemperaDevice is situated. If a limit transgression is detected, the climateMeasurement is
     * passed on to the warningService's checkWarning method.
     * @param climateMeasurements list of measurements to be checked coming in from RestController
     */
    public void checkLimits(List<ClimateMeasurement> climateMeasurements) {
        try {
            if (Duration.between(climateMeasurements.get(0).getTimeStamp(), LocalDateTime.now()).toMinutes() > 15) {
                //Measurement older than 15 minutes. Ignoring.
                return;
            }

            TemperaDevice temperaDevice = temperaDeviceService
                    .findTemperaDeviceBySensor(climateMeasurements.get(0).getSensor());

            Room room = temperaDevice.getAccessPoint().getRoom();

            List<Limits> limitsList = room.getLimitsList();

            for (ClimateMeasurement climateMeasurement : climateMeasurements) {
                Sensor sensor = climateMeasurement.getSensor();
                SensorType measurementType = sensor.getSensorType();

                Limits limits = limitService.findLimitBySensorType(limitsList, measurementType);
                double measuredValue = climateMeasurement.getMeasuredValue();

                if (limits.getLowerLimit() > measuredValue || limits.getUpperLimit() < measuredValue) {
                    warningService.checkWarning(climateMeasurement);
                }

            }
        } catch (NullPointerException e) {
            // no AccessPoint or Room configured, therefore no Limits and no warnings.
        }
    }
}
