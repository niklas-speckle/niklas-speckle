package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.ClimateMeasurementRepository;
import at.qe.skeleton.services.climate.ClimateMeasurementService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.climate.WarningService;
import at.qe.skeleton.services.room.LimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@WebAppConfiguration
class ClimateMeasurementServiceTest {

    @Autowired
    ClimateMeasurementService cmService;

    @MockBean
    ClimateMeasurementRepository cmRepository;

    @MockBean
    LimitService limitService;

    @MockBean
    TemperaDeviceService temperaDeviceService;

    @MockBean
    WarningService warningService;

    @Test
    void testSave() {
        ClimateMeasurement measurement = new ClimateMeasurement();
        cmService.save(measurement);
        verify(cmRepository, times(1)).save(measurement);
    }

    @Test
    void testSaveAll() {
        List<ClimateMeasurement> measurements = new ArrayList<>();
        cmService.saveAll(measurements);
        verify(cmRepository, times(1)).saveAll(measurements);
    }

    @Test
    void testDelete() {
        ClimateMeasurement measurement = new ClimateMeasurement();
        cmService.delete(measurement);
        verify(cmRepository, times(1)).delete(measurement);
    }

    @Test
    void testFindAllMeasurements() {
        List<ClimateMeasurement> allMeasurements = new ArrayList<>();
        when(cmRepository.findAll()).thenReturn(allMeasurements);

        List<ClimateMeasurement> foundMeasurements = cmService.findAllMeasurements();
        assertEquals(allMeasurements, foundMeasurements);
    }

    @Test
    void testFindMeasurementById() {
        long id = 1L;
        ClimateMeasurement measurement = new ClimateMeasurement();
        when(cmRepository.findById(id)).thenReturn(Optional.of(measurement));

        Optional<ClimateMeasurement> foundMeasurement = cmService.findMeasurementById(id);
        assertTrue(foundMeasurement.isPresent());
        assertEquals(measurement, foundMeasurement.get());
    }

    @Test
    void testFestFindAllMeasurementsByUser() {
        // Create test user with a TemperaDevice containing sensors
        Sensor sensor1 = new Sensor();
        sensor1.setId(1L);
        Sensor sensor2 = new Sensor();
        sensor2.setId(2L);
        List<Sensor> sensors = new ArrayList<>();
        sensors.add(sensor1);
        sensors.add(sensor2);

        TemperaDevice device = new TemperaDevice();
        device.setSensors(sensors);

        Userx user = new Userx();
        user.setTemperaDevice(device);

        // Create ClimateMeasurements for each sensor
        List<ClimateMeasurement> measurements1 = new ArrayList<>();
        measurements1.add(new ClimateMeasurement(1L, LocalDateTime.now(), sensor1, 20.0));
        measurements1.add(new ClimateMeasurement(2L, LocalDateTime.now().minusMinutes(1), sensor1, 25.0));

        List<ClimateMeasurement> measurements2 = new ArrayList<>();
        measurements2.add(new ClimateMeasurement(3L, LocalDateTime.now().minusMonths(1), sensor2, 50.0));

        // Mock repository behaviour
        when(cmRepository.findAllBySensor(sensor1)).thenReturn(measurements1);
        when(cmRepository.findAllBySensor(sensor2)).thenReturn(measurements2);

        // Calling service method
        List<ClimateMeasurement> allMeasurementsByUser = cmService.findAllMeasurementsByUser(user);

        List<ClimateMeasurement> expectedMeasurements = new ArrayList<>();
        expectedMeasurements.addAll(measurements1);
        expectedMeasurements.addAll(measurements2);

        assertEquals(expectedMeasurements.size(), allMeasurementsByUser.size(), "different sizes");
        assertTrue(expectedMeasurements.containsAll(allMeasurementsByUser), "doesn't contain all expected");
        verify(cmRepository, times(1)).findAllBySensor(sensor1);
        verify(cmRepository, times(1)).findAllBySensor(sensor2);
    }

    @Test
    void testFindCurrentMeasurementForUser_whenCurrentMeasurementExists_shouldReturnMeasurementValue() {
        // Create a user with a TemperaDevice containing a temperature sensor
        Sensor temperatureSensor = new Sensor();
        temperatureSensor.setId(1L);
        temperatureSensor.setSensorType(SensorType.AIR_TEMPERATURE);
        Userx user = new Userx();
        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(temperatureSensor));
        user.setTemperaDevice(device);

        // Define mock current measurement
        ClimateMeasurement mockMeasurement = new ClimateMeasurement();
        mockMeasurement.setMeasuredValue(25.0);

        // Mock the repository behavior to return the mock measurement
        when(cmRepository.findNewestBySensorAndNotOlderThan(anyLong(), any(LocalDateTime.class))).thenReturn(Optional.of(mockMeasurement));

        // Call service method
        Double result = cmService.findCurrentMeasurementForUser(user, SensorType.AIR_TEMPERATURE);

        verify(cmRepository, times(1)).findNewestBySensorAndNotOlderThan(eq(temperatureSensor.getId()), any(LocalDateTime.class));
        assertEquals(mockMeasurement.getMeasuredValue(), result);
    }

    @Test
    void testFindCurrentMeasurementForUser_whenNoCurrentMeasurementExists_shouldThrowNoSuchElementException() {
        Userx user = new Userx();

        when(cmRepository.findNewestBySensorAndNotOlderThan(anyLong(), any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> cmService.findCurrentMeasurementForUser(user, SensorType.AIR_TEMPERATURE));
    }

    @Test
    void testFindSensorMeasurementsBetween() {
        Sensor mockSensor = new Sensor();
        mockSensor.setId(1L);

        // start and end dates for time range
        LocalDate start = LocalDate.of(2024, 5, 1);
        LocalDate end = LocalDate.of(2024, 5, 3);

        // mock climate measurements within the specified time range
        List<ClimateMeasurement> mockMeasurements = new ArrayList<>();
        mockMeasurements.add(new ClimateMeasurement(1L, LocalDateTime.of(2024, 5, 1, 12, 0), mockSensor, 25.0));
        mockMeasurements.add(new ClimateMeasurement(2L, LocalDateTime.of(2024, 5, 2, 12, 0), mockSensor, 26.0));
        mockMeasurements.add(new ClimateMeasurement(3L, LocalDateTime.of(2024, 5, 3, 12, 0), mockSensor, 27.0));

        // Mock repository behavior
        when(cmRepository.findAllBySensorAndTimeStampAfterAndTimeStampBefore(eq(mockSensor), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockMeasurements);

        // Call the method under test
        List<ClimateMeasurement> result = cmService.findSensorMeasurementsBetween(mockSensor, start, end);

        // Verify that the repository method was called with the correct arguments
        verify(cmRepository, times(1)).findAllBySensorAndTimeStampAfterAndTimeStampBefore(mockSensor,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        // Verify that the returned measurements match the mock measurements
        assertEquals(mockMeasurements.size(), result.size());
        assertEquals(mockMeasurements, result);
    }

    @Test
    void testTemperatureHistoryForUser() {
        // Create test user with a TemperaDevice containing a humidity sensor
        Sensor temperatureSensor = new Sensor();
        temperatureSensor.setSensorType(SensorType.AIR_TEMPERATURE);
        Userx user = new Userx();
        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(temperatureSensor));
        user.setTemperaDevice(device);

        // create mock measurements
        List<ClimateMeasurement> mockMeasurements = new ArrayList<>();
        mockMeasurements.add(new ClimateMeasurement(1L, LocalDateTime.now().minusMinutes(5), temperatureSensor, 20.0));
        mockMeasurements.add(new ClimateMeasurement(2L, LocalDateTime.now().minusMinutes(10), temperatureSensor, 30.0));
        mockMeasurements.add(new ClimateMeasurement(3L, LocalDateTime.now().minusMinutes(15), temperatureSensor, 40.0));

        when(cmRepository.findAllBySensorAndTimeStampAfterAndTimeStampBefore(eq(temperatureSensor), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockMeasurements);

        // time range and granularity
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        int granularityInMinutes = 1; // test filtering for granularity via the dedicated test further down (tests for private methods)

        // Calling service method
        List<ClimateMeasurement> result = cmService.temperatureHistoryForUser(user, start, end, granularityInMinutes);

        // Verify that repository method was called with correct arguments
        verify(cmRepository, times(1)).findAllBySensorAndTimeStampAfterAndTimeStampBefore(temperatureSensor,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        // Verify that returned measurements match mock measurements
        assertEquals(mockMeasurements.size(), result.size(), "Size not as expected.");
        assertTrue(result.containsAll(mockMeasurements), "Result doesn't contain all expected measurements.");
    }

    @Test
    void testHumidityHistoryForUser() {
        // Create test user with a TemperaDevice containing a humidity sensor
        Sensor humiditySensor = new Sensor();
        humiditySensor.setSensorType(SensorType.AIR_HUMIDITY);
        Userx user = new Userx();
        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(humiditySensor));
        user.setTemperaDevice(device);

        // create mock measurements
        List<ClimateMeasurement> mockMeasurements = new ArrayList<>();
        mockMeasurements.add(new ClimateMeasurement(1L, LocalDateTime.now().minusMinutes(5), humiditySensor, 20.0));
        mockMeasurements.add(new ClimateMeasurement(2L, LocalDateTime.now().minusMinutes(10), humiditySensor, 30.0));
        mockMeasurements.add(new ClimateMeasurement(3L, LocalDateTime.now().minusMinutes(15), humiditySensor, 40.0));

        when(cmRepository.findAllBySensorAndTimeStampAfterAndTimeStampBefore(eq(humiditySensor), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockMeasurements);

        // time range and granularity
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        int granularityInMinutes = 1; // test filtering for granularity via the dedicated test further down (tests for private methods)

        // Calling service method
        List<ClimateMeasurement> result = cmService.humidityHistoryForUser(user, start, end, granularityInMinutes);

        // Verify that repository method was called with correct arguments
        verify(cmRepository, times(1)).findAllBySensorAndTimeStampAfterAndTimeStampBefore(humiditySensor,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        // Verify that returned measurements match mock measurements
        assertEquals(mockMeasurements.size(), result.size(), "Size not as expected.");
        assertTrue(result.containsAll(mockMeasurements), "Result doesn't contain all expected measurements.");
    }

    @Test
    void testAirQualityHistoryForUser() {
        // Create test user with a TemperaDevice containing an air quality sensor
        Sensor airQualitySensor = new Sensor();
        airQualitySensor.setSensorType(SensorType.AIR_QUALITY);
        Userx user = new Userx();
        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(airQualitySensor));
        user.setTemperaDevice(device);

        // create mock measurements
        List<ClimateMeasurement> mockMeasurements = new ArrayList<>();
        mockMeasurements.add(new ClimateMeasurement(1L, LocalDateTime.now().minusMinutes(5), airQualitySensor, 20.0));
        mockMeasurements.add(new ClimateMeasurement(2L, LocalDateTime.now().minusMinutes(10), airQualitySensor, 30.0));
        mockMeasurements.add(new ClimateMeasurement(3L, LocalDateTime.now().minusMinutes(15), airQualitySensor, 40.0));

        when(cmRepository.findAllBySensorAndTimeStampAfterAndTimeStampBefore(eq(airQualitySensor), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockMeasurements);

        // time range and granularity
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        int granularityInMinutes = 1; // test filtering for granularity via the dedicated test further down (tests for private methods)

        // Calling service method
        List<ClimateMeasurement> result = cmService.airQualityHistoryForUser(user, start, end, granularityInMinutes);

        // Verify that repository method was called with correct arguments
        verify(cmRepository, times(1)).findAllBySensorAndTimeStampAfterAndTimeStampBefore(airQualitySensor,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        // Verify that returned measurements match mock measurements
        assertEquals(mockMeasurements.size(), result.size(), "Size not as expected.");
        assertTrue(result.containsAll(mockMeasurements), "Result doesn't contain all expected measurements.");
    }


    @Test
    void testLightHistoryForUser() {
        // Create test user with a TemperaDevice containing a light sensor
        Sensor lightSensor = new Sensor();
        lightSensor.setSensorType(SensorType.LIGHT_INTENSITY);
        Userx user = new Userx();
        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(lightSensor));
        user.setTemperaDevice(device);

        // create mock measurements
        List<ClimateMeasurement> mockMeasurements = new ArrayList<>();
        mockMeasurements.add(new ClimateMeasurement(1L, LocalDateTime.now().minusMinutes(5), lightSensor, 20.0));
        mockMeasurements.add(new ClimateMeasurement(2L, LocalDateTime.now().minusMinutes(10), lightSensor, 30.0));
        mockMeasurements.add(new ClimateMeasurement(3L, LocalDateTime.now().minusMinutes(15), lightSensor, 40.0));

        when(cmRepository.findAllBySensorAndTimeStampAfterAndTimeStampBefore(eq(lightSensor), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockMeasurements);

        // time range and granularity
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        int granularityInMinutes = 1; // test filtering for granularity via the dedicated test further down (tests for private methods)

        // Calling service method
        List<ClimateMeasurement> result = cmService.lightHistoryForUser(user, start, end, granularityInMinutes);

        // Verify that repository method was called with correct arguments
        verify(cmRepository, times(1)).findAllBySensorAndTimeStampAfterAndTimeStampBefore(lightSensor,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        // Verify that returned measurements match mock measurements
        assertEquals(mockMeasurements.size(), result.size(), "Size not as expected.");
        assertTrue(result.containsAll(mockMeasurements), "Result doesn't contain all expected measurements.");
    }


    /*          TESTS FOR PRIVATE METHODS (need to be set to public for testing)

    @Test
    void testGetSensor() {
        // Create a user with a TemperaDevice containing all 4 types of sensors
        Userx user = new Userx();
        TemperaDevice device = new TemperaDevice();

        Sensor temperatureSensor = new Sensor();
        temperatureSensor.setSensorType(SensorType.AIR_TEMPERATURE);
        Sensor humiditySensor = new Sensor();
        humiditySensor.setSensorType(SensorType.AIR_HUMIDITY);
        Sensor airQualitySensor = new Sensor();
        airQualitySensor.setSensorType(SensorType.AIR_QUALITY);
        Sensor lightSensor = new Sensor();
        lightSensor.setSensorType(SensorType.LIGHT_INTENSITY);

        device.setSensors(List.of(temperatureSensor, humiditySensor, airQualitySensor, lightSensor));
        user.setTemperaDevice(device);

        // Test for AIR_TEMPERATURE sensor
        Sensor temperatureResult = cmService.getSensor(user, SensorType.AIR_TEMPERATURE);
        assertNotNull(temperatureResult);
        assertEquals(device.getTemperatureSensor(), temperatureSensor);

        // Test for AIR_HUMIDITY sensor
        Sensor humidityResult = cmService.getSensor(user, SensorType.AIR_HUMIDITY);
        assertNotNull(humidityResult);
        assertEquals(device.getHumiditySensor(), humiditySensor);

        // Test for AIR_QUALITY sensor
        Sensor airQualityResult = cmService.getSensor(user, SensorType.AIR_QUALITY);
        assertNotNull(airQualityResult);
        assertEquals(device.getAirQualitySensor(), airQualitySensor);

        // Test for LIGHT_INTENSITY sensor
        Sensor lightResult = cmService.getSensor(user, SensorType.LIGHT_INTENSITY);
        assertNotNull(lightResult);
        assertEquals(device.getLightSensor(), lightSensor);

        // Test for null TemperaDevice
        Userx userWithNullDevice = new Userx();
        assertThrows(NoSuchElementException.class, () -> cmService.getSensor(userWithNullDevice, SensorType.AIR_TEMPERATURE));
    }


    @Test
    void testFilterMeasurements() {
        // create temperature measurements
        Sensor sensor = new Sensor();
        ClimateMeasurement m1 = new ClimateMeasurement(1L, LocalDateTime.now().minusMinutes(10), sensor, 10.0);
        ClimateMeasurement m2 = new ClimateMeasurement(2L, LocalDateTime.now().minusMinutes(15), sensor, 15.0);
        ClimateMeasurement m3 = new ClimateMeasurement(3L, LocalDateTime.now().minusMinutes(20), sensor, 20.0);
        ClimateMeasurement m4 = new ClimateMeasurement(3L, LocalDateTime.now().minusMinutes(25), sensor, 25.0);
        List<ClimateMeasurement> measurements = List.of(m1, m2, m3, m4);

        List<ClimateMeasurement> filteredMeasurements = cmService.filterMeasurements(measurements, 1);
        assertEquals(measurements.size(), filteredMeasurements.size(), "All measurements should be present when filtering for a granularity of 1.");

        filteredMeasurements = cmService.filterMeasurements(measurements, 8);
        assertEquals(2, filteredMeasurements.size(), "2 measurements should be present when filtering for a granularity of 8.");
        assertTrue(filteredMeasurements.containsAll(List.of(m1, m3)), "m1 and m3 should be present when filtering for a granularity of 8.");

        filteredMeasurements = cmService.filterMeasurements(measurements, 30);
        assertEquals(1, filteredMeasurements.size(), "1 measurements should be present when filtering for a granularity of 30.");
        assertTrue(filteredMeasurements.contains(m1), "m1 should be present when filtering for a granularity of 30.");
    }

     */

    @Test
    public void testCheckLimitsWithViolations() {
        // Create TemperaDevice containing a temperature sensor
        Sensor temperatureSensor = new Sensor();
        temperatureSensor.setSensorType(SensorType.AIR_TEMPERATURE);

        Limits limits = new Limits();
        limits.setSensorType(SensorType.AIR_TEMPERATURE);
        limits.setLowerLimit(20.0);
        limits.setUpperLimit(27.0);

        Room room = new Room();
        room.setLimitsList(List.of(limits));

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setRoom(room);

        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(temperatureSensor));
        device.setAccessPoint(accessPoint);



        // Given
        ClimateMeasurement cmUpperViolation = new ClimateMeasurement();
        cmUpperViolation.setMeasuredValue(30.0);
        cmUpperViolation.setSensor(temperatureSensor);
        cmUpperViolation.setTimeStamp(LocalDateTime.now().minusMinutes(3));

        ClimateMeasurement cmLowerViolation = new ClimateMeasurement();
        cmLowerViolation.setMeasuredValue(18.0);
        cmLowerViolation.setSensor(temperatureSensor);
        cmLowerViolation.setTimeStamp(LocalDateTime.now().minusMinutes(3));

        List<ClimateMeasurement> measurements = new ArrayList<>();
        measurements.add(cmUpperViolation);
        measurements.add(cmLowerViolation);

        when(temperaDeviceService.findTemperaDeviceBySensor(any())).thenReturn(device);
        when(limitService.findLimitBySensorType(anyList(), eq(SensorType.AIR_TEMPERATURE))).thenReturn(limits);
        when(warningService.checkWarning(any())).thenReturn(null);

        // When
        cmService.checkLimits(measurements);

        // Then
        verify(temperaDeviceService, times(1)).findTemperaDeviceBySensor(any());
        verify(limitService, times(2)).findLimitBySensorType(anyList(), eq(SensorType.AIR_TEMPERATURE));
        verify(warningService, times(2)).checkWarning(any());
    }

    @Test
    public void testCheckLimitsWithoutViolations() {
        // Create TemperaDevice containing a humidity sensor
        Sensor humiditySensor = new Sensor();
        humiditySensor.setSensorType(SensorType.AIR_HUMIDITY);

        Limits limits = new Limits();
        limits.setSensorType(SensorType.AIR_HUMIDITY);
        limits.setLowerLimit(40.0);
        limits.setUpperLimit(60.0);

        Room room = new Room();
        room.setLimitsList(List.of(limits));

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setRoom(room);

        TemperaDevice device = new TemperaDevice();
        device.setSensors(List.of(humiditySensor));
        device.setAccessPoint(accessPoint);

        // Given
        ClimateMeasurement cmUpperViolation = new ClimateMeasurement();
        cmUpperViolation.setMeasuredValue(50.0);
        cmUpperViolation.setSensor(humiditySensor);
        cmUpperViolation.setTimeStamp(LocalDateTime.now().minusMinutes(3));

        ClimateMeasurement cmLowerViolation = new ClimateMeasurement();
        cmLowerViolation.setMeasuredValue(45.0);
        cmLowerViolation.setSensor(humiditySensor);
        cmLowerViolation.setTimeStamp(LocalDateTime.now().minusMinutes(3));

        List<ClimateMeasurement> measurements = new ArrayList<>();
        measurements.add(cmUpperViolation);
        measurements.add(cmLowerViolation);

        when(temperaDeviceService.findTemperaDeviceBySensor(any())).thenReturn(device);
        when(limitService.findLimitBySensorType(anyList(), eq(SensorType.AIR_HUMIDITY))).thenReturn(limits);
        when(warningService.checkWarning(any())).thenReturn(null);

        // When
        cmService.checkLimits(measurements);

        // Then
        verify(temperaDeviceService, times(1)).findTemperaDeviceBySensor(any());
        verify(limitService, times(2)).findLimitBySensorType(anyList(), eq(SensorType.AIR_HUMIDITY));
        verify(warningService, times(0)).checkWarning(any());
    }

    @Test
    public void testOldClimateMeasurementIsReturned(){
        // Create TemperaDevice containing a light sensor
        Sensor lightSensor = new Sensor();
        lightSensor.setSensorType(SensorType.LIGHT_INTENSITY);

        ClimateMeasurement cmViolation = new ClimateMeasurement();
        cmViolation.setMeasuredValue(50.0);
        cmViolation.setSensor(lightSensor);
        cmViolation.setTimeStamp(LocalDateTime.now().minusMinutes(16));

        // When
        cmService.checkLimits(List.of(cmViolation));

        // Then
        verify(temperaDeviceService, never()).findTemperaDeviceBySensor(any());
        verify(limitService, never()).findLimitBySensorType(anyList(), any());
        verify(warningService, never()).checkWarning(any());
    }

}