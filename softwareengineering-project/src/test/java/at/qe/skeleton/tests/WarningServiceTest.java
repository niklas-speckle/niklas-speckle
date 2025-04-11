package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import at.qe.skeleton.services.notifications.TokenService;
import at.qe.skeleton.services.climate.WarningService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
    @WebAppConfiguration
    public class WarningServiceTest {

        @Mock
        private TemperaDeviceRepository temperaDeviceRepository;

        @Mock
        private TimeRecordService timeRecordService;

        @Mock
        private WarningRepository warningRepository;

        @Mock
        private TemperaDeviceService temperaDeviceService;

        @Mock
        private ApplicationEventPublisher applicationEventPublisher;

        @Mock
        private TokenService tokenService;

        @InjectMocks
        private WarningService warningService;

        private Sensor sensor;
        private TemperaDevice temperaDevice;

        @BeforeEach
        public void setUp() {
            sensor = new Sensor();
            sensor.setSensorType(SensorType.AIR_TEMPERATURE);

            temperaDevice = new TemperaDevice();
            temperaDevice.setSensors(Arrays.asList(sensor));

            when(temperaDeviceService.findTemperaDeviceBySensor(any())).thenReturn(temperaDevice);
            doNothing().when(applicationEventPublisher).publishEvent(any()); //does not work
        }

        @Transactional
        @Test
        public void noActiveWarningResultsInNewWarningDraft() {
            // Given
            when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(null);

            ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                    .measuredValue(40.0)
                    .sensor(sensor)
                    .timeStamp(LocalDateTime.now())
                    .build();

            // When
            Warning createdWarning = warningService.checkWarning(climateMeasurement);

            // Then
            Assertions.assertNotNull(createdWarning);
            Assertions.assertEquals(WarningStatus.DRAFT, createdWarning.getWarningStatus());
            Assertions.assertNull(createdWarning.getToken());
            Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
            Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
            Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
        }

        @Transactional
        @Test
        public void oldWarningShouldBeReplacedByNewWarning(){
            //Given
            Token token = new Token("token", null);

            Warning oldWarning = Warning.builder()
                    .warningStatus(WarningStatus.UNSEEN)
                    .timestamp(LocalDateTime.now().minusHours(5))
                    .measuredValue(40.0)
                    .sensorType(sensor.getSensorType())
                    .token(token)
                    .build();

            temperaDevice.setWarnings(Arrays.asList(oldWarning));

            when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(oldWarning);
            when(tokenService.disableToken(any())).thenReturn(token);

            ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                    .sensor(sensor)
                    .timeStamp(LocalDateTime.now())
                    .measuredValue(40.0)
                    .build();

            // When
            Warning createdWarning = warningService.checkWarning(climateMeasurement);

            // Then
            verify(tokenService, times(1)).disableToken(token);
            Assertions.assertNotNull(createdWarning);
            Assertions.assertEquals(WarningStatus.DRAFT, createdWarning.getWarningStatus());
            Assertions.assertNull(createdWarning.getToken());
            Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
            Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
            Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
            Assertions.assertTrue(temperaDevice.getWarnings().contains(createdWarning), "Warning was not added to the device");
            Assertions.assertFalse(temperaDevice.getWarnings().contains(oldWarning), "Old warning was not removed from the device");

        }

    @Transactional
    @Test
    public void oldIgnoredWarningShouldBeReplacedByNewWarning(){
        //Given
        Token token = new Token("token", null);

        Warning ignoredWarning = Warning.builder()
                .warningStatus(WarningStatus.IGNORED)
                .timestamp(LocalDateTime.now().minusMinutes(65))
                .measuredValue(40.0)
                .sensorType(sensor.getSensorType())
                .token(token)
                .build();

        temperaDevice.setWarnings(Arrays.asList(ignoredWarning));

        when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(ignoredWarning);
        when(tokenService.disableToken(any())).thenReturn(token);

        ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                .sensor(sensor)
                .timeStamp(LocalDateTime.now())
                .measuredValue(35.0)
                .build();

        // When
        Warning createdWarning = warningService.checkWarning(climateMeasurement);

        // Then
        verify(tokenService, times(1)).disableToken(token);
        Assertions.assertNotNull(createdWarning);
        Assertions.assertEquals(WarningStatus.DRAFT, createdWarning.getWarningStatus());
        Assertions.assertNull(createdWarning.getToken());
        Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
        Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
        Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
        Assertions.assertTrue(temperaDevice.getWarnings().contains(createdWarning), "Warning was not added to the device");
        Assertions.assertFalse(temperaDevice.getWarnings().contains(ignoredWarning), "Old warning was not removed from the device");

    }

    @Transactional
    @Test
    public void oldConfirmedWarningShouldBeReplacedByNewWarning(){
        //Given
        Token token = new Token("token", null);

        Warning confirmedWarning = Warning.builder()
                .warningStatus(WarningStatus.CONFIRMED)
                .timestamp(LocalDateTime.now().minusMinutes(16))
                .measuredValue(40.0)
                .sensorType(sensor.getSensorType())
                .token(token)
                .build();

        temperaDevice.setWarnings(Arrays.asList(confirmedWarning));

        when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(confirmedWarning);
        when(tokenService.disableToken(any())).thenReturn(token);

        ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                .sensor(sensor)
                .timeStamp(LocalDateTime.now())
                .measuredValue(35.0)
                .build();

        // When
        Warning createdWarning = warningService.checkWarning(climateMeasurement);

        // Then
        verify(tokenService, times(1)).disableToken(token);
        Assertions.assertNotNull(createdWarning);
        Assertions.assertEquals(WarningStatus.DRAFT, createdWarning.getWarningStatus());
        Assertions.assertNull(createdWarning.getToken());
        Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
        Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
        Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
        Assertions.assertTrue(temperaDevice.getWarnings().contains(createdWarning), "Warning was not added to the device");
        Assertions.assertFalse(temperaDevice.getWarnings().contains(confirmedWarning), "Old warning was not removed from the device");

    }

    @Transactional
    @Test
    @DisplayName("Draft Warning should be set to unseen in case the violation lasts more than 5 minutes")
    public void draftWarningShouldBeSetToUnseen(){
        //Given
        Token token = new Token("token", null);

        Warning warningDraft = Warning.builder()
                .warningStatus(WarningStatus.DRAFT)
                .timestamp(LocalDateTime.now().minusMinutes(6))
                .measuredValue(30.0)
                .sensorType(sensor.getSensorType())
                .build();

        temperaDevice.setWarnings(Arrays.asList(warningDraft));

        when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(warningDraft);
        when(tokenService.checkToken(any())).thenReturn(token);

        ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                .sensor(sensor)
                .timeStamp(LocalDateTime.now())
                .measuredValue(35.0)
                .build();

        // When
        Warning createdWarning = warningService.checkWarning(climateMeasurement);

        // Then
        verify(tokenService, times(1)).checkToken(createdWarning);
        Assertions.assertNotNull(createdWarning);
        Assertions.assertEquals(WarningStatus.UNSEEN, createdWarning.getWarningStatus());
        Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
        Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
        Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
        Assertions.assertEquals(warningDraft, createdWarning);
    }

    @Transactional
    @Test
    @DisplayName("Unseen warning should be renewed in case the User is in the Room.")
    public void unseenWarningWithUserInRoom(){
        //Given
        Token token = new Token("token", null);

        Warning unseenWarning = Warning.builder()
                .warningStatus(WarningStatus.UNSEEN)
                .timestamp(LocalDateTime.now().minusMinutes(36))
                .measuredValue(30.0)
                .sensorType(sensor.getSensorType())
                .token(token)
                .build();

        temperaDevice.setWarnings(Arrays.asList(unseenWarning));

        when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(unseenWarning);
        when(temperaDeviceService.findUserOfTemperaDevice(any())).thenReturn(null);
        when(tokenService.disableToken(any())).thenReturn(token);
        when(timeRecordService.getCurrentWorkModeOfUser(any())).thenReturn(WorkMode.AVAILABLE);

        ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                .sensor(sensor)
                .timeStamp(LocalDateTime.now())
                .measuredValue(35.0)
                .build();

        // When
        Warning createdWarning = warningService.checkWarning(climateMeasurement);

        // Then
        verify(tokenService, times(1)).disableToken(any());
        Assertions.assertNotNull(createdWarning);
        Assertions.assertEquals(WarningStatus.DRAFT, createdWarning.getWarningStatus());
        Assertions.assertNull(createdWarning.getToken());
        Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
        Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
        Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
        Assertions.assertTrue(temperaDevice.getWarnings().contains(createdWarning), "Warning was not added to the device");
        Assertions.assertFalse(temperaDevice.getWarnings().contains(unseenWarning), "Old warning was not removed from the device");
    }

    @Transactional
    @Test
    @DisplayName("Unseen warning with no User in Room should be refresh its timestamp")
    public void unseenWarningWithNoUserInRoom(){
        //Given
        Token token = new Token("token", null);

        Warning unsseenWarning = Warning.builder()
                .warningStatus(WarningStatus.DRAFT)
                .timestamp(LocalDateTime.now().minusMinutes(36))
                .measuredValue(30.0)
                .sensorType(sensor.getSensorType())
                .token(token)
                .build();

        temperaDevice.setWarnings(Arrays.asList(unsseenWarning));

        when(temperaDeviceService.findUserOfTemperaDevice(any())).thenReturn(null);
        when(timeRecordService.getCurrentWorkModeOfUser(any())).thenReturn(WorkMode.MEETING);
        when(temperaDeviceService.getActiveWarning(any(), any())).thenReturn(unsseenWarning);

        ClimateMeasurement climateMeasurement = ClimateMeasurement.builder()
                .sensor(sensor)
                .timeStamp(LocalDateTime.now())
                .measuredValue(35.0)
                .build();

        // When
        Warning createdWarning = warningService.checkWarning(climateMeasurement);

        // Then
        Assertions.assertNotNull(createdWarning);
        Assertions.assertEquals(WarningStatus.UNSEEN, createdWarning.getWarningStatus());
        Assertions.assertEquals(climateMeasurement.getTimeStamp(), createdWarning.getTimestamp());
        Assertions.assertEquals(climateMeasurement.getSensor().getSensorType(), createdWarning.getSensorType());
        Assertions.assertEquals(climateMeasurement.getMeasuredValue(), createdWarning.getMeasuredValue());
        Assertions.assertEquals(unsseenWarning, createdWarning);
    }

    @Transactional
    @Test
    public void deleteIgnoredWarningTest(){
        //Given
        Token token = new Token("token", null);

        Warning warning = Warning.builder()
                .warningStatus(WarningStatus.IGNORED)
                .token(token)
                .build();

        TemperaDevice temperaDevice = TemperaDevice.builder()
                .warnings(Arrays.asList(warning, new Warning()))
                .build();

        when(temperaDeviceRepository.save(temperaDevice)).thenReturn(temperaDevice);
        when(tokenService.disableToken(token)).thenReturn(token);
        doNothing().when(applicationEventPublisher).publishEvent(any());
        doNothing().when(warningRepository).delete(warning);

        //When
        warningService.deleteWarning(warning, temperaDevice);

        //Then
        verify(tokenService, times(1)).disableToken(token);
        verify(applicationEventPublisher, times(1)).publishEvent(any());
        verify(warningRepository, times(1)).delete(warning);
        Assertions.assertEquals(1, temperaDevice.getWarnings().size());
        Assertions.assertFalse(temperaDevice.getWarnings().contains(warning));
    }

    @Transactional
    @Test
    public void deleteDraftWarningTest(){
        //Given
        Warning warning = Warning.builder()
                .warningStatus(WarningStatus.DRAFT)
                .build();

        TemperaDevice temperaDevice = TemperaDevice.builder()
                .warnings(Arrays.asList(warning, new Warning()))
                .build();

        when(temperaDeviceRepository.save(temperaDevice)).thenReturn(temperaDevice);
        doNothing().when(warningRepository).delete(warning);

        //When
        warningService.deleteWarning(warning, temperaDevice);

        //Then
        verify(tokenService, never()).disableToken(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
        verify(warningRepository, times(1)).delete(warning);
        Assertions.assertEquals(1, temperaDevice.getWarnings().size());
        Assertions.assertFalse(temperaDevice.getWarnings().contains(warning));
    }
}
