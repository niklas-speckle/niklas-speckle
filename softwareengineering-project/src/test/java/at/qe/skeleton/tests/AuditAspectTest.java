package at.qe.skeleton.tests;

import at.qe.skeleton.auditing.AuditAspect;
import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.APINotification;
import at.qe.skeleton.model.notifications.NotificationType;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.rest.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@WebAppConfiguration
public class AuditAspectTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkGroupRepository workGroupRepository;

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @InjectMocks
    private AuditAspect auditAspect;

    private Logger logger;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        logger = LoggerFactory.getLogger(AuditAspect.class);
    }

    // Helper method to mock authentication context
    private void mockAuthentication(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void testLogAuthenticationSuccess() {
        mockAuthentication("admin");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        InteractiveAuthenticationSuccessEvent event = new InteractiveAuthenticationSuccessEvent(authentication, this.getClass());

        auditAspect.logAuthenticationSuccess(event);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogAuthenticationFailure() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("admin");

        AuthenticationFailureBadCredentialsEvent event = new AuthenticationFailureBadCredentialsEvent(authentication, new BadCredentialsException("Bad credentials"));

        auditAspect.logAuthenticationFailure(event);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSaveUserSuccess() {
        Userx user = new Userx();
        user.setUsername("admin");

        Userx savedUser = new Userx();
        savedUser.setUsername("admin");

        mockAuthentication("admin");

        auditAspect.logSaveUserSuccess(user, savedUser);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveUserError() {
        Userx user = new Userx();
        user.setUsername("admin");

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveUserError(user, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogChangePasswordSuccess() {
        Userx user = new Userx();
        user.setUsername("admin");

        mockAuthentication("admin");

        auditAspect.logChangePasswordSuccess(user);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogChangePasswordError() {
        Userx user = new Userx();
        user.setUsername("admin");

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logChangePasswordError(user, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSetTemperaDeviceSuccess() {
        Userx user = new Userx();
        user.setUsername("admin");

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        mockAuthentication("admin");

        auditAspect.logSetTemperaDeviceSuccess(user, temperaDevice);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSetTemperaDeviceError() {
        Userx user = new Userx();
        user.setUsername("admin");

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSetTemperaDeviceError(user, temperaDevice, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteUser() {
        Userx user = new Userx();
        user.setUsername("admin");

        mockAuthentication("admin");

        auditAspect.logDeleteUser(user);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveProjectSuccess() {
        Project project = new Project();
        project.setName("Project1");

        Project savedProject = new Project();
        savedProject.setName("Project1");

        mockAuthentication("admin");

        auditAspect.logSaveProjectSuccess(project, savedProject);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveProjectError() {
        Project project = new Project();
        project.setName("Project1");

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveProjectError(project, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteProject() {
        Project project = new Project();
        project.setName("Project1");

        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(project));

        mockAuthentication("admin");

        auditAspect.logDeleteProject(1L);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSetProjectUsers() {
        Project project = new Project();
        project.setName("Project1");

        Userx user1 = new Userx();
        user1.setId(1L);
        Set<Userx> users = Set.of(user1);

        mockAuthentication("admin");

        auditAspect.logSetProjectUsers(project, users);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveWorkGroupSuccess() {
        WorkGroup workGroup = new WorkGroup();
        workGroup.setName("WorkGroup1");

        WorkGroup savedWorkGroup = new WorkGroup();
        savedWorkGroup.setName("WorkGroup1");

        mockAuthentication("admin");

        auditAspect.logSaveWorkGroupSuccess(workGroup, savedWorkGroup);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveWorkGroupError() {
        WorkGroup workGroup = new WorkGroup();
        workGroup.setName("WorkGroup1");

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveWorkGroupError(workGroup, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteWorkGroupSuccess() {
        WorkGroup workGroup = new WorkGroup();
        workGroup.setName("WorkGroup1");

        when(workGroupRepository.findById(anyLong())).thenReturn(Optional.of(workGroup));

        mockAuthentication("admin");

        auditAspect.logDeleteWorkGroupSuccess(1L);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogDeleteWorkGroupError() {
        WorkGroup workGroup = new WorkGroup();
        workGroup.setName("WorkGroup1");

        when(workGroupRepository.findById(anyLong())).thenReturn(Optional.of(workGroup));
        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logDeleteWorkGroupError(1L, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSaveRoomSuccess() {
        Room room = new Room();
        room.setRoomNumber("Room1");

        Room savedRoom = new Room();
        savedRoom.setRoomNumber("Room1");

        mockAuthentication("admin");

        auditAspect.logSaveRoomSuccess(room, savedRoom);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveRoomError() {
        Room room = new Room();
        room.setRoomNumber("Room1");

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveRoomError(room, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteRoomSuccess() {
        Room room = new Room();
        room.setRoomNumber("Room1");

        mockAuthentication("admin");

        auditAspect.logDeleteRoomSuccess(room);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogDeleteRoomError() {
        Room room = new Room();
        room.setRoomNumber("Room1");

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logDeleteRoomError(room, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSaveLimitsSuccess() {
        Limits limits = new Limits();
        limits.setId(1L);
        Room room = new Room();
        room.setRoomNumber("Room1");
        limits.setRoom(room);

        mockAuthentication("admin");

        auditAspect.logSaveLimitsSuccess(limits);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogDeleteLimitsSuccess() {
        Limits limits = new Limits();
        limits.setId(1L);
        Room room = new Room();
        room.setRoomNumber("Room1");
        limits.setRoom(room);

        mockAuthentication("admin");

        auditAspect.logDeleteLimitsSuccess(limits);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveAccessPointSuccess() {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(1L);

        AccessPoint savedAccessPoint = new AccessPoint();
        savedAccessPoint.setId(1L);

        mockAuthentication("admin");

        auditAspect.logSaveAccessPointSuccess(accessPoint, savedAccessPoint);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveAccessPointError() {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveAccessPointError(accessPoint, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteAccessPointSuccess() {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(1L);

        mockAuthentication("admin");

        auditAspect.logDeleteAccessPointSuccess(accessPoint);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogDeleteAccessPointError() {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logDeleteAccessPointError(accessPoint, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSaveTemperaDeviceSuccess() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        TemperaDevice savedTemperaDevice = new TemperaDevice();
        savedTemperaDevice.setId(1L);

        mockAuthentication("admin");

        auditAspect.logSaveTemperaDeviceSuccess(temperaDevice, savedTemperaDevice);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveTemperaDeviceError() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveTemperaDeviceError(temperaDevice, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteTemperaDeviceSuccess() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        mockAuthentication("admin");

        auditAspect.logDeleteTemperaDeviceSuccess(temperaDevice);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogDeleteTemperaDeviceError() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logDeleteTemperaDeviceError(temperaDevice, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogAPConnectionLost() {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(1L);

        mockAuthentication("admin");

        auditAspect.logAPConnectionLost(accessPoint);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogAPConnection() {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setId(1L);

        APINotification notification = new APINotification();
        notification.setNotificationType(NotificationType.INFO);
        notification.setMessage("Connection established");

        mockAuthentication("admin");

        auditAspect.logAPConnection(accessPoint, notification);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogCreateTimeRecordFailure() {
        TimeRecordDTO timeRecordDTO = new TimeRecordDTO(null, null, null, null);

        ResponseEntity<TimeRecordDTO> response = new ResponseEntity<>(HttpStatus.FORBIDDEN);

        mockAuthentication("admin");

        auditAspect.logCreateTimeRecordFailure(timeRecordDTO, "1", response);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogCreateMeasurementFailure() {
        MeasurementDTO measurementDTO = new MeasurementDTO(null, null, null, 10.0f, 10.0f, 10.5f, 10.5f);

        ResponseEntity<MeasurementDTO> response = new ResponseEntity<>(HttpStatus.FORBIDDEN);

        mockAuthentication("admin");

        auditAspect.logCreateMeasurementFailure(measurementDTO, "1", response);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogGetLogTemperaDeviceByAccessPointIdFailure() {
        ResponseEntity<LogTemperaDeviceDTO> response = new ResponseEntity<>(HttpStatus.FORBIDDEN);

        mockAuthentication("admin");

        auditAspect.logGetLogTemperaDeviceByAccessPointIdFailure("1", response);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeleteLogTemperaDeviceByAccessPointIdFailure() {
        ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.FORBIDDEN);

        mockAuthentication("admin");

        auditAspect.logDeleteLogTemperaDeviceByAccessPointIdFailure("1", response);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogDeviceMessages() {
        APINotificationDTO apiNotificationDTO = new APINotificationDTO(LocalDateTime.now(),"1", Integer.toUnsignedLong(1), "1", "Test Message");

        ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.FORBIDDEN);

        mockAuthentication("admin");

        auditAspect.logDeviceMessages(apiNotificationDTO, "1", response);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSaveTimeRecordSuccess() {
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setId(1L);

        TimeRecord savedTimeRecord = new TimeRecord();
        savedTimeRecord.setId(1L);

        mockAuthentication("admin");

        auditAspect.logSaveTimeRecordSuccess(timeRecord, savedTimeRecord);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveTimeRecordError() {
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveTimeRecordError(timeRecord, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogEditTimeRecordSuccess() {
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setId(1L);

        TimeRecord editedTimeRecord = new TimeRecord();
        editedTimeRecord.setId(1L);

        mockAuthentication("admin");

        auditAspect.logEditTimeRecordSuccess(timeRecord, editedTimeRecord);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogEditTimeRecordError() {
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logEditTimeRecordError(timeRecord, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSplitTimeRecordSuccess() {
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setId(1L);

        TimeRecord splitRecord1 = new TimeRecord();
        splitRecord1.setId(2L);
        TimeRecord splitRecord2 = new TimeRecord();
        splitRecord2.setId(3L);

        mockAuthentication("admin");

        auditAspect.logSplitTimeRecordSuccess(timeRecord, LocalDateTime.now(), List.of(splitRecord1, splitRecord2));

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSplitTimeRecordError() {
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logEditTimeRecordError(timeRecord, LocalDateTime.now(), ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSaveNewAndCloseOldTimeRecordSuccess() {
        Userx user = new Userx();
        user.setUsername("admin");

        TimeRecord newTimeRecord = new TimeRecord();
        newTimeRecord.setId(2L);
        newTimeRecord.setUser(user);

        TimeRecord oldTimeRecord = new TimeRecord();
        oldTimeRecord.setId(1L);
        oldTimeRecord.setUser(user);

        when(timeRecordRepository.findFirstByUserAndEndTimeIsNull(any(Userx.class))).thenReturn(oldTimeRecord);

        mockAuthentication("admin");

        auditAspect.logSaveNewAndCloseOldTimeRecordSuccess(newTimeRecord);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSaveNewAndCloseOldTimeRecordError() {
        Userx user = new Userx();
        user.setUsername("admin");

        TimeRecord newTimeRecord = new TimeRecord();
        newTimeRecord.setId(2L);
        newTimeRecord.setUser(user);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logSaveNewAndCloseOldTimeRecordError(newTimeRecord, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogUpdateWarningStatusSuccess() {
        Warning warning = new Warning();
        warning.setId(1L);

        WarningStatus warningStatus = WarningStatus.UNSEEN;

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        mockAuthentication("admin");

        auditAspect.logUpdateWarningStatusSuccess(warning, warningStatus, temperaDevice);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogUpdateWarningStatusError() {
        Warning warning = new Warning();
        warning.setId(1L);

        WarningStatus warningStatus = WarningStatus.UNSEEN;

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setId(1L);

        Exception ex = new Exception("Error");

        mockAuthentication("admin");

        auditAspect.logUpdateWarningStatusError(warning, warningStatus, temperaDevice, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }

    @Test
    public void testLogSendEmailSuccess() {
        mockAuthentication("admin");

        Userx sendTo = new Userx();
        sendTo.setEmail("user@example.com");
        String subject = "Test Subject";
        String message = "Test Message";

        auditAspect.logSendEmailSuccess(sendTo, subject, message);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isInfoEnabled();
    }

    @Test
    public void testLogSendEmailError() {
        mockAuthentication("admin");

        Userx sendTo = new Userx();
        sendTo.setEmail("user@example.com");
        String subject = "Test Subject";
        String message = "Test Message";
        Exception ex = new Exception("Error");

        auditAspect.logSendEmailError(sendTo, subject, message, ex);

        verify(logRepository, times(1)).save(any(AuditLog.class));
        assert logger.isErrorEnabled();
    }
}
