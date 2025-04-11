package at.qe.skeleton.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import at.qe.skeleton.model.ClimateMeasurement;
import at.qe.skeleton.model.Sensor;
import at.qe.skeleton.model.SensorType;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.rest.dto.MeasurementDTO;
import at.qe.skeleton.rest.mapper.MeasurementMapper;
import at.qe.skeleton.services.TemperaDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

class MeasurementMapperTest {

    @Mock
    private TemperaDeviceService temperaDeviceService;

    @InjectMocks
    private MeasurementMapper measurementMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMapFromWithNullDTOShouldReturnEmptyList() {
        assertEquals(0, measurementMapper.mapFrom(null).size());
    }

    @Test
    public void testMapFromWithValidDTOShouldMapCorrectly() {
        MeasurementDTO dto = new MeasurementDTO(1L, LocalDateTime.now(), 1L,
                20.5f, 20.5f, 20.5f, 20.5f);

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        Sensor sensor1 = new Sensor();
        sensor1.setId(1L);
        sensor1.setSensorType(SensorType.AIR_TEMPERATURE);

        Sensor sensor2 = new Sensor();
        sensor2.setId(2L);
        sensor2.setSensorType(SensorType.AIR_HUMIDITY);

        Sensor sensor3 = new Sensor();
        sensor3.setId(3L);
        sensor3.setSensorType(SensorType.AIR_QUALITY);

        Sensor sensor4 = new Sensor();
        sensor4.setId(4L);
        sensor4.setSensorType(SensorType.LIGHT_INTENSITY);

        temperaDevice.setSensors(List.of(sensor1, sensor2, sensor3, sensor4));

        when(temperaDeviceService.findTemperaDeviceById(1L)).thenReturn(temperaDevice);

        List<ClimateMeasurement> climateMeasurements = measurementMapper.mapFrom(dto);

        assertEquals(dto.timestamp(), climateMeasurements.get(0).getTimeStamp());
        assertEquals(dto.airTemperature(), climateMeasurements.get(0).getMeasuredValue());
        assertEquals(dto.timestamp(), climateMeasurements.get(1).getTimeStamp());
        assertEquals(dto.airHumidity(), climateMeasurements.get(1).getMeasuredValue());
        assertEquals(dto.timestamp(), climateMeasurements.get(2).getTimeStamp());
        assertEquals(dto.airQuality(), climateMeasurements.get(2).getMeasuredValue());
        assertEquals(dto.timestamp(), climateMeasurements.get(3).getTimeStamp());
        assertEquals(dto.lightIntensity(), climateMeasurements.get(3).getMeasuredValue());


    }


}