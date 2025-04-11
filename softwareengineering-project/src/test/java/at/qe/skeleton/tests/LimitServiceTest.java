package at.qe.skeleton.tests;

import at.qe.skeleton.model.Limits;
import at.qe.skeleton.model.SensorType;
import at.qe.skeleton.services.room.LimitService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

@SpringBootTest
@WebAppConfiguration
public class LimitServiceTest {

    @Autowired
    private LimitService limitService;

    /**
     *
     */
    @Test
    public void testAreLimitsValid() {

        Limits limit1 = new Limits();
        limit1.setLowerLimit(0.0);
        limit1.setUpperLimit(100.0);

        Limits limit2 = new Limits();
        limit2.setLowerLimit(50.0);
        limit2.setUpperLimit(100.0);

        List<Limits> validLimits = List.of(limit1, limit2);

        // Test valid limits
        Assertions.assertTrue(limitService.areLimitsValid(validLimits), "Expected all limits to be valid");

        Limits limit3 = new Limits();
        limit3.setLowerLimit(100.0);
        limit3.setUpperLimit(50.0);

        List<Limits> invalidLimits = List.of(limit1, limit2, limit3);

        // Test invalid limits
        Assertions.assertFalse(limitService.areLimitsValid(invalidLimits), "Expected some limits to be invalid");
    }


    @ParameterizedTest(name = "findLimitBySensorType")
    @CsvSource({
            "AIR_TEMPERATURE, 20.0, 27.0",
            "AIR_HUMIDITY, 40.0, 60.0",
            "AIR_QUALITY, 20.0, 70.0",
            "LIGHT_INTENSITY, 40.0, 60.0"
    })
    public void testFindLimitBySensorType(SensorType sensorType, double lowerLimit, double upperLimit) {
        //Given
        Limits temperatureLimits = new Limits();
        temperatureLimits.setSensorType(SensorType.AIR_TEMPERATURE);
        temperatureLimits.setLowerLimit(20.0);
        temperatureLimits.setUpperLimit(27.0);

        Limits humidityLimits = new Limits();
        humidityLimits.setSensorType(SensorType.AIR_HUMIDITY);
        humidityLimits.setLowerLimit(40.0);
        humidityLimits.setUpperLimit(60.0);

        Limits qualityLimits = new Limits();
        qualityLimits.setSensorType(SensorType.AIR_QUALITY);
        qualityLimits.setLowerLimit(20.0);
        qualityLimits.setUpperLimit(70.0);

        Limits lightLimits = new Limits();
        lightLimits.setSensorType(SensorType.LIGHT_INTENSITY);
        lightLimits.setLowerLimit(40.0);
        lightLimits.setUpperLimit(60.0);

        List<Limits> limitsList = List.of(temperatureLimits, humidityLimits, qualityLimits, lightLimits);

        //When
        Limits retrievedLimits = limitService.findLimitBySensorType(limitsList, sensorType);

        //Then
        Assertions.assertEquals(sensorType, retrievedLimits.getSensorType());
        Assertions.assertEquals(lowerLimit, retrievedLimits.getLowerLimit());
        Assertions.assertEquals(upperLimit, retrievedLimits.getUpperLimit());
    }

    @Test
    public void testFindLimitBySensorTypeNull() {
        //Given
        Limits temperatureLimits = new Limits();
        temperatureLimits.setSensorType(SensorType.AIR_TEMPERATURE);
        temperatureLimits.setLowerLimit(20.0);
        temperatureLimits.setUpperLimit(27.0);

        Limits humidityLimits = new Limits();
        humidityLimits.setSensorType(SensorType.AIR_HUMIDITY);
        humidityLimits.setLowerLimit(40.0);
        humidityLimits.setUpperLimit(60.0);

        Limits qualityLimits = new Limits();
        qualityLimits.setSensorType(SensorType.AIR_QUALITY);
        qualityLimits.setLowerLimit(20.0);
        qualityLimits.setUpperLimit(70.0);

        List<Limits> limitsList = List.of(temperatureLimits, humidityLimits, qualityLimits);

        //When
        Limits retrievedLimits = limitService.findLimitBySensorType(limitsList, SensorType.LIGHT_INTENSITY);

        //Then
        Assertions.assertNull(retrievedLimits);
    }

    @Test
    public void testFindLimitBySensorTypeEmpty() {
        //Given
        List<Limits> limitsList = List.of();

        //When
        Limits retrievedLimits = limitService.findLimitBySensorType(limitsList, SensorType.LIGHT_INTENSITY);

        //Then
        Assertions.assertNull(retrievedLimits);
    }

    @Test
    public void testFindLimitBySensorTypeNullList() {
        //When
        Limits retrievedLimits = limitService.findLimitBySensorType(null, SensorType.LIGHT_INTENSITY);

        //Then
        Assertions.assertNull(retrievedLimits);
    }
}