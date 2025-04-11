package at.qe.skeleton.tests;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.APINotificationAPBody;
import at.qe.skeleton.rest.controllers.RestController;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import at.qe.skeleton.rest.dto.LogTemperaDeviceDTO;
import at.qe.skeleton.rest.dto.MeasurementDTO;
import at.qe.skeleton.rest.mapper.APINotificationDeviceBodyDTOMapper;
import at.qe.skeleton.rest.mapper.LogTemperaDeviceDTOMapper;
import at.qe.skeleton.rest.mapper.MeasurementMapper;
import at.qe.skeleton.services.*;
import at.qe.skeleton.services.climate.ClimateMeasurementService;
import at.qe.skeleton.services.climate.WarningService;
import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.services.notifications.TokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RestControllerTest {
    @Mock
    private AccessPointService accessPointService;

    @Mock
    private MeasurementMapper measurementMapper;

    @Mock
    private ClimateMeasurementService climateMeasurementService;

    @Mock
    private LogTemperaDeviceDTOMapper logTemperaDeviceDTOMapper;

    @Mock
    private TokenService tokenService;

    @Mock
    private WarningService warningService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TemperaDeviceService temperaDeviceService;

    @Mock
    private APINotificationDeviceBodyDTOMapper apiNotificationDeviceBodyDTOMapper;

    @InjectMocks
    private RestController restController;


    @Test
    public void testCreateMeasurementInvalidAccessPointId() throws EntityValidationException{
        // Given
        MeasurementDTO measurementDTO = new MeasurementDTO(null, null, null, 0, 0, 0, 0);
        String accessPointId = "invalidAccessPointId";

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenThrow(new IllegalArgumentException());

        // When
        ResponseEntity<MeasurementDTO> response = restController.createMeasurement(measurementDTO, accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testCreateMeasurementDatabaseError() throws EntityValidationException {
        // Given
        MeasurementDTO measurementDTO = new MeasurementDTO(null, null, null, 0, 0, 0, 0);
        String accessPointId = "validAccessPointId";
        AccessPoint accessPoint = new AccessPoint();
        List<ClimateMeasurement> climateMeasurements = List.of(new ClimateMeasurement());

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);
        when(measurementMapper.mapFrom(measurementDTO)).thenReturn(climateMeasurements);
        when(climateMeasurementService.saveAll(climateMeasurements)).thenThrow(new DataIntegrityViolationException("Database error"));

        // When
        ResponseEntity<MeasurementDTO> response = restController.createMeasurement(measurementDTO, accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testCreateMeasurementSuccess() throws EntityValidationException{
        // Given
        MeasurementDTO measurementDTO = new MeasurementDTO(null, null, null, 0, 0, 0, 0);
        String accessPointId = "validAccessPointId";
        AccessPoint accessPoint = new AccessPoint();
        List<ClimateMeasurement> climateMeasurements = List.of(new ClimateMeasurement());

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);
        when(measurementMapper.mapFrom(measurementDTO)).thenReturn(climateMeasurements);
        when(climateMeasurementService.saveAll(climateMeasurements)).thenReturn(climateMeasurements);
        doNothing().when(climateMeasurementService).checkLimits(climateMeasurements);

        // When
        ResponseEntity<MeasurementDTO> response = restController.createMeasurement(measurementDTO, accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertEquals(measurementDTO, response.getBody());
        verify(climateMeasurementService).saveAll(climateMeasurements);
        verify(climateMeasurementService).checkLimits(climateMeasurements);
    }

    @Test
    public void testGetLogTemperaDeviceByAccessPointId_Success() throws EntityValidationException {
        // Given
        String accessPointId = "validAccessPointId";
        AccessPoint accessPoint = new AccessPoint();
        LogTemperaDevice logTemperaDevice = new LogTemperaDevice();
        accessPoint.setLogTemperaDevices(List.of(logTemperaDevice));
        LogTemperaDeviceDTO logTemperaDeviceDTO = new LogTemperaDeviceDTO(LocalDateTime.now(), null, 0L, null);

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);
        when(logTemperaDeviceDTOMapper.mapTo(logTemperaDevice)).thenReturn(logTemperaDeviceDTO);

        // When
        ResponseEntity<LogTemperaDeviceDTO> response = restController.getLogTemperaDeviceByAccessPointId(accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(logTemperaDeviceDTO, response.getBody());
    }

    @Test
    public void testGetLogTemperaDeviceByAccessPointIdInvalidAccessPointId() throws EntityValidationException {
        // Given
        String accessPointId = "invalidAccessPointId";

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenThrow(new IllegalArgumentException());

        // When
        ResponseEntity<LogTemperaDeviceDTO> response = restController.getLogTemperaDeviceByAccessPointId(accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testGetLogTemperaDeviceByAccessPointIdDatabaseError() throws EntityValidationException {
        // Given
        String accessPointId = "validAccessPointId";
        LogTemperaDevice logTemperaDevice = new LogTemperaDevice();
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLogTemperaDevices(List.of(logTemperaDevice));

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);
        when(logTemperaDeviceDTOMapper.mapTo(any())).thenThrow(new DataAccessException("Database error") {});

        // When
        ResponseEntity<LogTemperaDeviceDTO> response = restController.getLogTemperaDeviceByAccessPointId(accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testGetLogTemperaDeviceByAccessPointIdNoLogTemperaDevice() throws EntityValidationException {
        // Given
        String accessPointId = "validAccessPointId";
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLogTemperaDevices(List.of());

        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);

        // When
        ResponseEntity<LogTemperaDeviceDTO> response = restController.getLogTemperaDeviceByAccessPointId(accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void testDeleteLogTemperaDeviceByAccessPointIdSuccess() throws EntityValidationException {
        // Given
        String accessPointId = "validAccessPointId";
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLogTemperaDevices(List.of(new LogTemperaDevice()));

        doNothing().when(accessPointService).resetLogTemperaDevice(accessPoint);
        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);

        // When
        ResponseEntity<String> response = restController.deleteLogTemperaDeviceByAccessPointId(accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("Deleted"));
        verify(accessPointService).resetLogTemperaDevice(accessPoint);
    }

    @Test
    public void testDeleteLogTemperaDeviceByAccessPointIdInvalidAccessPointId() throws EntityValidationException {
        // Given
        String accessPointId = "invalidAccessPointId";
        when(accessPointService.getActiveAccessPointById(accessPointId)).thenThrow(new IllegalArgumentException());

        // Testfall
        ResponseEntity<String> response = restController.deleteLogTemperaDeviceByAccessPointId(accessPointId);

        // Überprüfen
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testDeleteLogTemperaDeviceByAccessPointIdDatabaseError() throws EntityValidationException {
        // Given
        String accessPointId = "validAccessPointId";
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLogTemperaDevices(List.of(new LogTemperaDevice()));

        doThrow(new DataAccessException("Database error") {}).when(accessPointService).resetLogTemperaDevice(accessPoint);
        when(accessPointService.getActiveAccessPointById(accessPointId)).thenReturn(accessPoint);

        // When
        ResponseEntity<String> response = restController.deleteLogTemperaDeviceByAccessPointId(accessPointId);

        // Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testUpdateWarningStatus_Success() {
        // Given
        String token = "validToken";
        Integer status = 2;
        Warning warning = new Warning();
        warning.setSensorType(SensorType.AIR_TEMPERATURE);
        when(tokenService.isTokenValid(token)).thenReturn(true);
        when(tokenService.getWarningByToken(token)).thenReturn(warning);
        doNothing().when(warningService).updateWarningStatus(warning.getId(), status);

        // When
        ResponseEntity<String> response = restController.updateWarningStatus(token, status);

        // Then
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().contains("IGNORED"));

        verify(warningService).updateWarningStatus(warning.getId(), status);
    }

    @Test
    public void testUpdateWarningStatus_InvalidStatus() {
        // Given
        String token = "validToken";
        Integer status = 1;
        Warning warning = new Warning();

        // When
        ResponseEntity<String> response = restController.updateWarningStatus(token, status);

        // Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(warningService, never()).updateWarningStatus(anyLong(), anyInt());
    }

    @Test
    public void testUpdateWarningStatusInvalidToken() {
        // Given
        String token = "invalidToken";
        Integer status = 3;
        when(tokenService.isTokenValid(token)).thenReturn(false);

        // When
        ResponseEntity<String> response = restController.updateWarningStatus(token, status);

        // Then
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testUpdateWarningStatusNoWarning() {
        // Given
        String token = "invalidToken";
        Integer status = 3;
        when(tokenService.isTokenValid(token)).thenReturn(true);
        when(tokenService.getWarningByToken(token)).thenReturn(null);

        // When
        ResponseEntity<String> response = restController.updateWarningStatus(token, status);

        // Then
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(warningService, never()).updateWarningStatus(anyLong(), anyInt());
        verify(tokenService).getWarningByToken(token);
    }

    @Test
    public void testUpdateWarningStatusDatabaseError() {
        // Given
        String token = "validToken";
        Integer status = 2;
        Warning warning = new Warning();
        when(tokenService.isTokenValid(token)).thenReturn(true);
        when(tokenService.getWarningByToken(token)).thenReturn(warning);
        doThrow(new DataAccessException("Database error") {
        }).when(warningService).updateWarningStatus(warning.getId(), status);

        // When
        ResponseEntity<String> response = restController.updateWarningStatus(token, status);

        // Then
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }


    private void mockSuccessfullValidationForCreateMessage() throws EntityValidationException, IdNotFoundException {

        doCallRealMethod().when(apiNotificationDeviceBodyDTOMapper).mapFrom(any(APINotificationDTO.class));

        when(accessPointService.isValidAccessPointIdString(any())).thenReturn(true);
        when(accessPointService.doesAccessPointIDExist(any())).thenReturn(true);
        when(accessPointService.getNotRegisteredAccessPointById(any())).thenReturn(null);
        when(accessPointService.isAccessPointEnabled(any())).thenReturn(true);
        when(accessPointService.isTemperaDeviceConnectedToAccessPoint(any(), any())).thenReturn(true);
        doNothing().when(accessPointService).updateLastConnection(any());

        when(notificationService.doAccessPointAndAPINotificationMatch(any(AccessPoint.class), any(APINotificationAPBody.class))).thenReturn(true);
        doNothing().when(notificationService).addNotificationToBell(any(), any());
        doNothing().when(notificationService).notificationFromAccessPoint(any());
    }

    @Test
    @Transactional
    public void testCreateMessageAP() throws EntityValidationException, IdNotFoundException, EntityStillInUseException {


        mockSuccessfullValidationForCreateMessage();

        AccessPoint accessPoint = AccessPoint.builder().id(1L).build();

        APINotificationDTO apiNotificationDTO_valid = APINotificationDTO.builder()
                .deviceType("AP")
                .deviceId(1L)
                .notificationType("1")
                .message("Test message")
                .build();

        String senderDeviceID = "G4T2-AP-1";

        when(accessPointService.getAccessPointById(senderDeviceID)).thenReturn(accessPoint);
        when(accessPointService.getAccessPointById(accessPoint.getId())).thenReturn(accessPoint);

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTO_valid, senderDeviceID);

        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @Test
    @Transactional
    public void testCreateMessageTD() throws EntityValidationException, IdNotFoundException, EntityStillInUseException {


        mockSuccessfullValidationForCreateMessage();

        AccessPoint accessPoint = AccessPoint.builder().id(1L).build();

        TemperaDevice temperaDevice = TemperaDevice.builder().id(1L).status(DeviceStatus.ENABLED).build();

        when(temperaDeviceService.findTemperaDeviceById(1L)).thenReturn(temperaDevice);

        APINotificationDTO apiNotificationDTO_valid = APINotificationDTO.builder()
                .deviceType("TD")
                .deviceId(1L)
                .notificationType("1")
                .message("Test message")
                .build();

        String senderDeviceID = "G4T2-AP-1";

        when(accessPointService.getAccessPointById(senderDeviceID)).thenReturn(accessPoint);
        when(accessPointService.getAccessPointById(accessPoint.getId())).thenReturn(accessPoint);

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTO_valid, senderDeviceID);

        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

    }

}
