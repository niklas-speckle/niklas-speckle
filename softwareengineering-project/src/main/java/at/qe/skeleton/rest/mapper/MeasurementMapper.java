package at.qe.skeleton.rest.mapper;

import at.qe.skeleton.model.ClimateMeasurement;
import at.qe.skeleton.model.Sensor;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.rest.dto.MeasurementDTO;
import at.qe.skeleton.services.climate.ClimateMeasurementService;
import at.qe.skeleton.services.TemperaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MeasurementMapper {

    @Autowired
    ClimateMeasurementService measurementService;

    @Autowired
    TemperaDeviceService temperaDeviceService;


    /**
     * maps a given MeasurementDTO to ClimateMeasurements.
     * The transmitted MeasurementDTO contains all values for the different sensors of a TemperaDevice measured at the
     * same time. The timestamp is used for all measurements. The respective Sensors are identified by the transmitted
     * TemperaDeviceId. The measured values are set according to the transmitted values: airTemperature for the
     * TemperatureSensor, airHumidity for the HumiditySensor, airQuality for the GasSensor and lightIntensity for the
     * LightSensor.
     * @param dto MeasurementDTO the MeasurementDTO to be mapped
     * @return the mapped ClimateMeasurements as a List
     */
    public List<ClimateMeasurement> mapFrom(MeasurementDTO dto) {
        if (dto == null) {
            return new ArrayList<>();
        }

        List<ClimateMeasurement> climateMeasurements = new ArrayList<>();

        ClimateMeasurement temperatureMeasurement = new ClimateMeasurement();
        ClimateMeasurement humidityMeasurement = new ClimateMeasurement();
        ClimateMeasurement gasMeasurement = new ClimateMeasurement();
        ClimateMeasurement lightMeasurement = new ClimateMeasurement();

        TemperaDevice temperaDevice = temperaDeviceService.findTemperaDeviceById(dto.temperaDeviceId());
        List<Sensor> sensors = temperaDevice.getSensors();

        temperatureMeasurement.setTimeStamp(dto.timestamp());
        temperatureMeasurement.setMeasuredValue(dto.airTemperature());

        humidityMeasurement.setTimeStamp(dto.timestamp());
        humidityMeasurement.setMeasuredValue(dto.airHumidity());

        gasMeasurement.setTimeStamp(dto.timestamp());
        gasMeasurement.setMeasuredValue(dto.airQuality());

        lightMeasurement.setTimeStamp(dto.timestamp());
        lightMeasurement.setMeasuredValue(dto.lightIntensity());

        for (Sensor sensor : sensors) {
            switch (sensor.getSensorType()) {
                case AIR_TEMPERATURE:
                    temperatureMeasurement.setSensor(sensor);
                    climateMeasurements.add(temperatureMeasurement);
                    break;
                case AIR_HUMIDITY:
                    humidityMeasurement.setSensor(sensor);
                    climateMeasurements.add(humidityMeasurement);
                    break;
                case AIR_QUALITY:
                    gasMeasurement.setSensor(sensor);
                    climateMeasurements.add(gasMeasurement);
                    break;
                case LIGHT_INTENSITY:
                    lightMeasurement.setSensor(sensor);
                    climateMeasurements.add(lightMeasurement);
                    break;
            }
        }

        return climateMeasurements;
    }
}
