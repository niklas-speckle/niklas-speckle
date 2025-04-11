package at.qe.skeleton.auditing;

import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.APINotification;
import at.qe.skeleton.repositories.LogRepository;
import at.qe.skeleton.repositories.ProjectRepository;
import at.qe.skeleton.repositories.TimeRecordRepository;
import at.qe.skeleton.repositories.WorkGroupRepository;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import at.qe.skeleton.rest.dto.LogTemperaDeviceDTO;
import at.qe.skeleton.rest.dto.MeasurementDTO;
import at.qe.skeleton.rest.dto.TimeRecordDTO;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AuditAspect is responsible for logging audit information for various service methods.
 * It uses Spring AOP to intercept method calls and log details about method executions,
 * including successes and failures.
 * As the logs are written after the methods have been called (and the saving process is complete), the log methods do not rely on isNew().
 * Instead, they check if createdBy and updatedBy are the same, which is only true after the first time an entity has been saved.
 */

/* sources:
https://medium.com/@AlexanderObregon/how-to-create-an-audit-trail-in-spring-boot-applications-3ecacd362825
https://docs.spring.io/spring-framework/docs/4.3.15.RELEASE/spring-framework-reference/html/aop.html#aop-understanding-aop-proxies
 */

@Aspect
@Component
public class AuditAspect {

    // Logger for general audit logging
    private static final Logger logger = LoggerFactory.getLogger("generalLogger");
    // Logger for specific error logging
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");
    public static final String USER = "USER: ";
    public static final String PROJECT = "PROJECT: ";
    public static final String WORKGROUP = "WORKGROUP: ";
    public static final String ROOM = "ROOM: ";
    public static final String LIMITS = "LIMITS: ";
    public static final String ACCESS_POINT = "ACCESS POINT: ";
    public static final String TEMPERA_DEVICE = "TEMPERA DEVICE: ";
    public static final String TIME_RECORD = "TIME_RECORD: ";
    public static final String WARNING = "WARNING: ";
    public static final String EMAIL = "EMAIL: ";
    public static final String ID = " id: ";


    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkGroupRepository workGroupRepository;

    @Autowired
    private TimeRecordRepository timeRecordRepository;


    // Authentication Logging

    /**
     * Logs the successful login of a user.
     *
     * @param event The authentication success event.
     */
    @EventListener
    public void logAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();

        AuditLog log = new AuditLog();
        log.setAction(Action.LOGIN);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(USER + username);
        log.setAuthenticatedUser(username);
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs the unsuccessful login attempt of a user.
     *
     * @param event The authentication failure event.
     */
    @EventListener
    public void logAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();

        AuditLog log = new AuditLog();
        log.setAction(Action.LOGIN);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(USER + username);
        log.setAuthenticatedUser("unauthenticated user");
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Bad credentials");
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }


// Logging for UserService

    /**
     * Logs the successful saving of a User.
     *
     * @param user      The user being saved.
     * @param savedUser The saved user.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.UserService.saveUser(..)) && args(user)",
            argNames= "user, savedUser",
            returning="savedUser")
    public void logSaveUserSuccess(Userx user, Userx savedUser) {
        AuditLog log = new AuditLog();
        if (user.getUpdateDate() == user.getCreateDate()) {
            log.setAction(Action.CREATE);
        } else {
            log.setAction(Action.UPDATE);
        }
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(USER +savedUser.getUsername()+ ID +user.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }


    /**
     * Logs an error when saving a User fails.
     *
     * @param user The user being saved.
     * @param ex   The exception that was thrown.
     */
    @AfterThrowing(pointcut="execution(* at.qe.skeleton.services.UserService.saveUser(..)) && args(user)",
            argNames= "user, ex",
            throwing="ex")
    public void logSaveUserError(Userx user, Exception ex) {
        AuditLog log = new AuditLog();
        if (user.getUpdateDate() == user.getCreateDate()) {
            log.setAction(Action.CREATE);
        } else {
            log.setAction(Action.UPDATE);
        }
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(USER +user.getUsername()+ ID +user.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful change of a User's password.
     *
     * @param user      The user being saved.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.UserService.changePassword(..)) && args(user)",
            argNames= "user")
    public void logChangePasswordSuccess(Userx user) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(USER +user.getUsername()+ ID +user.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Changed password.");
        logRepository.save(log);
        logger.info("{}", log);
    }


    /**
     * Logs an error when saving a User fails.
     *
     * @param user The user being saved.
     * @param ex   The exception that was thrown.
     */
    @AfterThrowing(pointcut="execution(* at.qe.skeleton.services.UserService.changePassword(..)) && args(user)",
            argNames= "user, ex",
            throwing="ex")
    public void logChangePasswordError(Userx user, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(USER +user.getUsername()+ ID +user.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Error while changing password: "+ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful setting of a TemperaDevice for a User.
     *
     * @param user          The user whose device is being set.
     * @param temperaDevice The TemperaDevice being set.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.UserService.setTemperaDevice(..)) && args(user, temperaDevice)",
                   argNames= "user, temperaDevice")
    public void logSetTemperaDeviceSuccess(Userx user, TemperaDevice temperaDevice) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        if(user != null){
            log.setAccessedResource(USER +user.getUsername()+ ID +user.getId() + TEMPERA_DEVICE + temperaDevice.getId());
        }
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Set TemperaDevice.");
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when setting a TemperaDevice for a User fails.
     *
     * @param user          The user whose device is being set.
     * @param temperaDevice The TemperaDevice being set.
     * @param ex            The exception that was thrown.
     */
    @AfterThrowing(pointcut="execution(* at.qe.skeleton.services.UserService.setTemperaDevice(..)) && args(user, temperaDevice)",
            argNames= "user, temperaDevice, ex",
            throwing="ex")
    public void logSetTemperaDeviceError(Userx user, TemperaDevice temperaDevice, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(USER +user.getUsername()+ ID +user.getId() + TEMPERA_DEVICE + temperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Error while setting TemperaDevice: "+ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }


    /**
     * Logs the successful deletion of a User.
     *
     * @param user The user being deleted.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.UserService.deleteUser(..)) && args(user)",
            argNames= "user")
    public void logDeleteUser(Userx user) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(USER +user.getUsername()+ ID +user.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }





    // Logging for ProjectService
    /**
     * Logs the successful saving of a Project.
     *
     * @param project      The project being saved.
     * @param savedProject The saved project.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.timeTracking.ProjectService.save(..)) && args(project)",
            argNames= "project, savedProject",
            returning="savedProject")
    public void logSaveProjectSuccess(Project project, Project savedProject) {
        AuditLog log = new AuditLog();
        if (project.getUpdateDate() == project.getCreateDate()) {
            log.setAction(Action.CREATE);
        } else {
            log.setAction(Action.UPDATE);
        }
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(PROJECT + savedProject.getName() + ID +project.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving a Project fails.
     *
     * @param project The project being saved.
     * @param ex      The exception that was thrown.
     */
    @AfterThrowing(pointcut="execution(* at.qe.skeleton.services.timeTracking.ProjectService.save(..)) && args(project)",
            argNames= "project, ex",
            throwing="ex")
    public void logSaveProjectError(Project project, Exception ex) {
        AuditLog log = new AuditLog();
        if (project.getUpdateDate() == project.getCreateDate()) {
            log.setAction(Action.CREATE);
        } else {
            log.setAction(Action.UPDATE);
        }
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(PROJECT + project.getName() + ID +project.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful deletion of a Project.
     *
     * @param id The ID of the project being deleted.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.timeTracking.ProjectService.delete(..)) && args(id)",
            argNames= "id")
    public void logDeleteProject(Long id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DELETE);
            log.setStatus(ActionStatus.SUCCESS);
            log.setAccessedResource(PROJECT + project.get().getName() + ID +project.get().getId());
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            logRepository.save(log);
            logger.info("{}", log);
        }
    }

    /**
     * Logs the successful setting of users for a Project.
     *
     * @param project The project whose users are being set.
     * @param users   The users being set.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.timeTracking.ProjectService.setUsers(..)) && args(project, users)",
            argNames= "project, users")
    public void logSetProjectUsers(Project project, Set<Userx> users) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(PROJECT + project.getName() + ID +project.getId() +" USERS: " + users.stream().map(Userx::getId).map(String::valueOf).collect(Collectors.joining(", ")));
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Set users.");
        logRepository.save(log);
        logger.info("{}", log);
    }






    // Logging for WorkGroupService
    /**
     * Logs the successful saving of a WorkGroup.
     *
     * @param workGroup      The work group being saved.
     * @param savedWorkGroup The saved work group.
     */

    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.timeTracking.WorkGroupService.save(..)) && args(workGroup)",
            argNames= "workGroup, savedWorkGroup",
            returning="savedWorkGroup")
    public void logSaveWorkGroupSuccess(WorkGroup workGroup, WorkGroup savedWorkGroup) {
        AuditLog log = new AuditLog();
        if (workGroup.getUpdateDate() == workGroup.getCreateDate()) {
            log.setAction(Action.CREATE);
        } else {
            log.setAction(Action.UPDATE);
        }
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(WORKGROUP + savedWorkGroup.getName() + ID +workGroup.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving a WorkGroup fails.
     *
     * @param workGroup The work group being saved.
     * @param ex        The exception that was thrown.
     */
    @AfterThrowing(pointcut="execution(* at.qe.skeleton.services.timeTracking.WorkGroupService.save(..)) && args(workGroup)",
            argNames= "workGroup, ex",
            throwing="ex")
    public void logSaveWorkGroupError(WorkGroup workGroup, Exception ex) {
        AuditLog log = new AuditLog();
        if (workGroup.getUpdateDate() == workGroup.getCreateDate()) {
            log.setAction(Action.CREATE);
        } else {
            log.setAction(Action.UPDATE);
        }
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(WORKGROUP + workGroup.getName() + ID +workGroup.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful deletion of a WorkGroup.
     *
     * @param id The ID of the work group being deleted.
     */
    @AfterReturning(pointcut="execution(* at.qe.skeleton.services.timeTracking.WorkGroupService.delete(..)) && args(id)",
            argNames= "id")
    public void logDeleteWorkGroupSuccess(Long id) {
        Optional<WorkGroup> workGroup = workGroupRepository.findById(id);
        if (workGroup.isPresent()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DELETE);
            log.setStatus(ActionStatus.SUCCESS);
            log.setAccessedResource(WORKGROUP + workGroup.get().getName() + ID +workGroup.get().getId());
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            logRepository.save(log);
            logger.info("{}", log);
        }
    }

    /**
     * Logs an error when deleting a WorkGroup fails.
     *
     * @param id The ID of the work group being deleted.
     * @param ex The exception that was thrown.
     */
    @AfterThrowing(pointcut="execution(* at.qe.skeleton.services.timeTracking.WorkGroupService.delete(..)) && args(id)",
            argNames= "id, ex",
            throwing="ex")
    public void logDeleteWorkGroupError(Long id, Exception ex) {
        Optional<WorkGroup> workGroup = workGroupRepository.findById(id);
        if (workGroup.isPresent()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DELETE);
            log.setStatus(ActionStatus.ERROR);
            log.setAccessedResource(WORKGROUP + workGroup.get().getName() + ID +workGroup.get().getId());
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            log.setAdditionalDetails(ex.getMessage());
            logRepository.save(log);
            String logMessage = log.toString();
            logger.info("{}", logMessage);
            errorLogger.error("{}", logMessage);
        }
    }


// Logging for RoomService

    /**
     * Logs the successful saving of a Room.
     *
     * @param room      The room being saved.
     * @param savedRoom The saved room.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.room.RoomService.saveRoom(..)) && args(room)",
            argNames = "room, savedRoom",
            returning = "savedRoom")
    public void logSaveRoomSuccess(Room room, Room savedRoom) {
        AuditLog log = new AuditLog();
        log.setAction(room.getUpdateDate() == room.getCreateDate() ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(ROOM + savedRoom.getRoomNumber());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving a Room fails.
     *
     * @param room The room being saved.
     * @param ex   The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.room.RoomService.saveRoom(..)) && args(room)",
            argNames = "room, ex",
            throwing = "ex")
    public void logSaveRoomError(Room room, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(room.getUpdateDate() == room.getCreateDate() ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(ROOM + room.getRoomNumber());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful deletion of a Room.
     *
     * @param room The room being deleted.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.room.RoomService.deleteRoom(..)) && args(room)",
            argNames = "room")
    public void logDeleteRoomSuccess(Room room) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(ROOM + room.getRoomNumber());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when deleting a Room fails.
     *
     * @param room The room being deleted.
     * @param ex   The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.room.RoomService.deleteRoom(..)) && args(room)",
            argNames = "room, ex",
            throwing = "ex")
    public void logDeleteRoomError(Room room, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(ROOM + room.getRoomNumber());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    // Logging for Limits

    /**
     * Logs the successful saving of a Limits entity.
     *
     * @param limits      The limits being saved.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.room.LimitService.saveLimit(..)) && args(limits)",
            argNames = "limits")
    public void logSaveLimitsSuccess(Limits limits) {

            AuditLog log = new AuditLog();
            log.setAction(limits.getReasonForChange()==null ? Action.CREATE : Action.UPDATE);
            log.setStatus(ActionStatus.SUCCESS);
            log.setAccessedResource(LIMITS + limits.getId() + ' '+ ROOM + limits.getRoom().getRoomNumber());
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            if (log.getAction() == Action.UPDATE) {
                log.setAdditionalDetails(limits.getReasonForChange());
            }
            logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs the successful deletion of a Limits entity.
     *
     * @param limits      The limits being deleted.
     */
    // LimitService doesn't provide delete method, deletion is done via CascadeType.ALL and orphan removal, so LimitsRepository is intercepted instead
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.repositories.LimitsRepository.delete(..)) && args(limits)",
            argNames = "limits")
    public void logDeleteLimitsSuccess(Limits limits) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DELETE);
            log.setStatus(ActionStatus.SUCCESS);
            log.setAccessedResource(LIMITS + limits.getId() + ' '+ ROOM  + limits.getRoom().getRoomNumber());
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            logRepository.save(log);
            logger.info("{}", log);
    }


    // Logging for AccessPointService

    /**
     * Logs the successful saving of an AccessPoint.
     *
     * @param accessPoint      The access point being saved.
     * @param savedAccessPoint The saved access point.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.AccessPointService.save(..)) && args(accessPoint)",
            argNames = "accessPoint, savedAccessPoint",
            returning = "savedAccessPoint")
    public void logSaveAccessPointSuccess(AccessPoint accessPoint, AccessPoint savedAccessPoint) {
        AuditLog log = new AuditLog();
        log.setAction(accessPoint.getUpdateDate() == accessPoint.getCreateDate() ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(ACCESS_POINT + savedAccessPoint.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving an AccessPoint fails.
     *
     * @param accessPoint The access point being saved.
     * @param ex          The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.AccessPointService.save(..)) && args(accessPoint)",
            argNames = "accessPoint, ex",
            throwing = "ex")
    public void logSaveAccessPointError(AccessPoint accessPoint, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(accessPoint.getUpdateDate() == accessPoint.getCreateDate() ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(ACCESS_POINT + accessPoint.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful deletion of an AccessPoint.
     *
     * @param accessPoint The access point being deleted.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.AccessPointService.delete(..)) && args(accessPoint)",
            argNames = "accessPoint")
    public void logDeleteAccessPointSuccess(AccessPoint accessPoint) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(ACCESS_POINT + accessPoint.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when deleting an AccessPoint fails.
     *
     * @param accessPoint The access point being deleted.
     * @param ex          The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.AccessPointService.delete(..)) && args(accessPoint)",
            argNames = "accessPoint, ex",
            throwing = "ex")
    public void logDeleteAccessPointError(AccessPoint accessPoint, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(ACCESS_POINT + accessPoint.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }


    // Logging for TemperaDeviceService

    /**
     * Logs the successful saving of a TemperaDevice.
     *
     * @param temperaDevice      The tempera device being saved.
     * @param savedTemperaDevice The saved tempera device.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.TemperaDeviceService.save(..)) && args(temperaDevice)",
            argNames = "temperaDevice, savedTemperaDevice",
            returning = "savedTemperaDevice")
    public void logSaveTemperaDeviceSuccess(TemperaDevice temperaDevice, TemperaDevice savedTemperaDevice) {
        AuditLog log = new AuditLog();
        log.setAction(temperaDevice.getUpdateDate() == temperaDevice.getCreateDate() ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(TEMPERA_DEVICE + savedTemperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving a TemperaDevice fails.
     *
     * @param temperaDevice The tempera device being saved.
     * @param ex            The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.TemperaDeviceService.save(..)) && args(temperaDevice)",
            argNames = "temperaDevice, ex",
            throwing = "ex")
    public void logSaveTemperaDeviceError(TemperaDevice temperaDevice, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(temperaDevice.getUpdateDate() == temperaDevice.getCreateDate() ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(TEMPERA_DEVICE + temperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful deletion of a TemperaDevice.
     *
     * @param temperaDevice The tempera device being deleted.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.TemperaDeviceService.delete(..)) && args(temperaDevice)",
            argNames = "temperaDevice")
    public void logDeleteTemperaDeviceSuccess(TemperaDevice temperaDevice) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(TEMPERA_DEVICE + temperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when deleting a TemperaDevice fails.
     *
     * @param temperaDevice The tempera device being deleted.
     * @param ex            The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.TemperaDeviceService.delete(..)) && args(temperaDevice)",
            argNames = "temperaDevice, ex",
            throwing = "ex")
    public void logDeleteTemperaDeviceError(TemperaDevice temperaDevice, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DELETE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(TEMPERA_DEVICE + temperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    // Logs for device connection

    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.AccessPointService.setDisconnected(..)) && args(accessPoint)",
            argNames = "accessPoint")

    public void logAPConnectionLost(AccessPoint accessPoint) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DEVICE_CONNECTION);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(ACCESS_POINT + accessPoint.getId());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Connection to AccessPoint has been lost.");
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.notifications.NotificationService.addNotificationToBell(..)) && args(device, notification)",
            argNames = "device, notification")
    public void logAPConnection(Device device, APINotification notification) {
        AuditLog log = new AuditLog();
        log.setAction(Action.DEVICE_CONNECTION);
        if (notification.getNotificationType() == null) {
            log.setStatus(ActionStatus.ERROR);
        } else {
            switch (notification.getNotificationType()) {
                case INFO -> log.setStatus(ActionStatus.SUCCESS);
                case WARNING -> log.setStatus(ActionStatus.WARNING);
                default -> log.setStatus(ActionStatus.ERROR);
            }
        }
        if (device instanceof TemperaDevice temperaDevice) {
            log.setAccessedResource(TEMPERA_DEVICE + temperaDevice.getId());
        } else if (device instanceof AccessPoint accessPoint) {
            log.setAccessedResource(ACCESS_POINT + accessPoint.getId());
        } else {
            log.setAccessedResource("UNKNOWN_DEVICE");
        }
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(notification.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        if (log.getStatus() != ActionStatus.SUCCESS) {
            errorLogger.error("{}", logMessage);
        }
    }

    /**
     * Logs an error when creating a TimeRecord fails to return a successful response.
     *
     * @param timeRecordDTO The TimeRecordDTO being processed.
     * @param accessPointId The ID of the AccessPoint.
     * @param response The ResponseEntity returned by the method.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.rest.controllers.RestController.createTimeRecord(..)) && args(timeRecordDTO, accessPointId)",
            argNames = "timeRecordDTO, accessPointId, response",
            returning = "response")
    public void logCreateTimeRecordFailure(TimeRecordDTO timeRecordDTO, String accessPointId, ResponseEntity<TimeRecordDTO> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DEVICE_CONNECTION);
            log.setStatus(ActionStatus.ERROR);
            log.setAccessedResource(ACCESS_POINT + accessPointId);
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                log.setAdditionalDetails("AccessPoint tried to create a time record but is not active/not found/not valid.");
            } else if (response.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)){
                log.setAdditionalDetails("AccessPoint tried to create a time record but an internal server error occurred. DataAccessException was thrown.");
            }
            logRepository.save(log);
            String logMessage = log.toString();
            logger.info("{}", logMessage);
            errorLogger.error("{}", logMessage);
        }
    }

    /**
     * Logs an error when creating a ClimateMeasurement fails to return a successful response.
     *
     * @param measurementDTO The MeasurementDTO being processed.
     * @param accessPointId The ID of the AccessPoint.
     * @param response The ResponseEntity returned by the method.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.rest.controllers.RestController.createMeasurement(..)) && args(measurementDTO, accessPointId)",
            argNames = "measurementDTO, accessPointId, response",
            returning = "response")
    public void logCreateMeasurementFailure(MeasurementDTO measurementDTO, String accessPointId, ResponseEntity<MeasurementDTO> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DEVICE_CONNECTION);
            log.setStatus(ActionStatus.ERROR);
            log.setAccessedResource(ACCESS_POINT + accessPointId);
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                log.setAdditionalDetails("AccessPoint tried to create a measurement but is not active/not found/not valid.");
            } else if (response.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                log.setAdditionalDetails("AccessPoint tried to create a measurement but an internal server error occurred. DataAccessException was thrown.");
            }
            logRepository.save(log);
            String logMessage = log.toString();
            logger.info("{}", logMessage);
            errorLogger.error("{}", logMessage);
        }
    }

    /**
     * Logs an error when getting LogTemperaDevice by AccessPointId fails to return a successful response.
     *
     * @param accessPointId The ID of the AccessPoint.
     * @param response The ResponseEntity returned by the method.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.rest.controllers.RestController.getLogTemperaDeviceByAccessPointId(..)) && args(accessPointId)",
            argNames = "accessPointId, response",
            returning = "response")
    public void logGetLogTemperaDeviceByAccessPointIdFailure(String accessPointId, ResponseEntity<LogTemperaDeviceDTO> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DEVICE_CONNECTION);
            log.setStatus(ActionStatus.ERROR);
            log.setAccessedResource(ACCESS_POINT + accessPointId);
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                log.setAdditionalDetails("AccessPoint tried to get LogTemperaDevice but is not active/not found/not valid.");
            } else if (response.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                log.setAdditionalDetails("AccessPoint tried to get LogTemperaDevice but an internal server error occurred. DataAccessException was thrown.");
            }
            logRepository.save(log);
            String logMessage = log.toString();
            logger.info("{}", logMessage);
            errorLogger.error("{}", logMessage);
        }
    }

    /**
     * Logs an error when deleting LogTemperaDevice by AccessPointId fails to return a successful response.
     *
     * @param accessPointId The ID of the AccessPoint.
     * @param response The ResponseEntity returned by the method.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.rest.controllers.RestController.deleteLogTemperaDeviceByAccessPointId(..)) && args(accessPointId)",
            argNames = "accessPointId, response",
            returning = "response")
    public void logDeleteLogTemperaDeviceByAccessPointIdFailure(String accessPointId, ResponseEntity<String> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DEVICE_CONNECTION);
            log.setStatus(ActionStatus.ERROR);
            log.setAccessedResource(ACCESS_POINT + accessPointId);
            log.setAuthenticatedUser(getAuthenticatedUser());
            log.setTimestamp(LocalDateTime.now());
            if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                log.setAdditionalDetails("AccessPoint tried to delete LogTemperaDevice but is not active/not found/not valid.");
            } else if (response.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                log.setAdditionalDetails("AccessPoint tried to delete LogTemperaDevice but an internal server error occurred. DataAccessException was thrown.");
            }
            logRepository.save(log);
            String logMessage = log.toString();
            logger.info("{}", logMessage);
            errorLogger.error("{}", logMessage);
        }
    }



    @AfterReturning(pointcut = "execution(* at.qe.skeleton.rest.controllers.RestController.createMessage(..)) && args(apiNotificationDTO, deviceID)",
            argNames = "apiNotificationDTO,deviceID,response",
            returning = "response")

    public void logDeviceMessages(APINotificationDTO apiNotificationDTO, String deviceID, ResponseEntity<String> response) {
            AuditLog log = new AuditLog();
            log.setAction(Action.DEVICE_CONNECTION);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.setStatus(ActionStatus.ERROR);
            } else {
                switch (apiNotificationDTO.notificationType()) {
                    case "1" -> log.setStatus(ActionStatus.SUCCESS);
                    case "2" -> log.setStatus(ActionStatus.WARNING);
                    default -> log.setStatus(ActionStatus.ERROR);
                }
            }
            log.setAccessedResource(ACCESS_POINT + deviceID);
            log.setTimestamp(LocalDateTime.now());
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.setAdditionalDetails(response.getBody());
        } else {
            log.setAdditionalDetails(apiNotificationDTO.message());
        }
            logRepository.save(log);
            String logMessage = log.toString();
            logger.info("{}", logMessage);
            errorLogger.error("{}", logMessage);
    }



// Logging for TimeRecordService

/**
 * Logs the successful saving of a TimeRecord.
 *
 * @param timeRecord      The time record being saved.
 * @param savedTimeRecord The saved time record.
 */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.saveTimeRecord(..)) && args(timeRecord)",
            argNames = "timeRecord, savedTimeRecord",
            returning = "savedTimeRecord")
    public void logSaveTimeRecordSuccess(TimeRecord timeRecord, TimeRecord savedTimeRecord) {
        AuditLog log = new AuditLog();
        log.setAction(timeRecord.getId() == null ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(TIME_RECORD + savedTimeRecord.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving a TimeRecord fails.
     *
     * @param timeRecord The time record being saved.
     * @param ex         The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.saveTimeRecord(..)) && args(timeRecord)",
            argNames = "timeRecord, ex",
            throwing = "ex")
    public void logSaveTimeRecordError(TimeRecord timeRecord, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(timeRecord.getId() == null ? Action.CREATE : Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(TIME_RECORD + timeRecord.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful editing of a TimeRecord.
     *
     * @param timeRecord      The time record being edited.
     * @param editedTimeRecord The edited time record.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.editTimeRecord(..)) && args(timeRecord)",
            argNames = "timeRecord, editedTimeRecord",
            returning = "editedTimeRecord")
    public void logEditTimeRecordSuccess(TimeRecord timeRecord, TimeRecord editedTimeRecord) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(TIME_RECORD + editedTimeRecord.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when editing a TimeRecord fails.
     *
     * @param timeRecord The time record being edited.
     * @param ex         The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.editTimeRecord(..)) && args(timeRecord)",
            argNames = "timeRecord, ex",
            throwing = "ex")
    public void logEditTimeRecordError(TimeRecord timeRecord, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(TIME_RECORD + timeRecord.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful splitting of a TimeRecord.
     *
     * @param timeRecord      The time record being split.
     * @param splitAt         The time at which the record was split.
     * @param splitTimeRecords The split time records.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.splitTimeRecord(..)) && args(timeRecord, splitAt)",
            argNames = "timeRecord, splitAt, splitTimeRecords",
            returning = "splitTimeRecords")
    public void logSplitTimeRecordSuccess(TimeRecord timeRecord, LocalDateTime splitAt, List<TimeRecord> splitTimeRecords) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(TIME_RECORD + timeRecord.getId() + " split at " + splitAt);
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Split into: " + splitTimeRecords.stream().map(TimeRecord::getId).map(String::valueOf).collect(Collectors.joining(", ")));
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when splitting a TimeRecord fails.
     *
     * @param timeRecord The time record being split.
     * @param splitAt    The time at which the record was split.
     * @param ex         The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.splitTimeRecord(..)) && args(timeRecord, splitAt)",
            argNames = "timeRecord, splitAt, ex",
            throwing = "ex")
    public void logEditTimeRecordError(TimeRecord timeRecord, LocalDateTime splitAt, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(TIME_RECORD + timeRecord.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    /**
     * Logs the successful saving of a new TimeRecord and closing of an old TimeRecord.
     *
     * @param newTimeRecord      The new time record being saved.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.saveNewAndCloseOldTimeRecord(..)) && args(newTimeRecord)",
            argNames = "newTimeRecord")
    public void logSaveNewAndCloseOldTimeRecordSuccess(TimeRecord newTimeRecord) {
        TimeRecord oldTimeRecord = timeRecordRepository.findFirstByUserAndEndTimeIsNull(newTimeRecord.getUser());
        AuditLog log = new AuditLog();
        log.setAction(Action.CREATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(TIME_RECORD + newTimeRecord.getId() + " for user: " + newTimeRecord.getUser().getUsername());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        if (oldTimeRecord != null) {
            log.setAdditionalDetails("Closed old TimeRecord: " + oldTimeRecord.getId() + " at: " + newTimeRecord.getStartTime());
        }
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when saving a new TimeRecord and closing an old TimeRecord fails.
     *
     * @param newTimeRecord The new time record being saved.
     * @param ex            The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.timeTracking.TimeRecordService.saveNewAndCloseOldTimeRecord(..)) && args(newTimeRecord)",
            argNames = "newTimeRecord, ex",
            throwing = "ex")
    public void logSaveNewAndCloseOldTimeRecordError(TimeRecord newTimeRecord, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.CREATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(TIME_RECORD + newTimeRecord.getId() + " for user: " + newTimeRecord.getUser().getUsername());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Error: " + ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    // Logs for Warning

    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.climate.WarningService.updateWarningStatus(..)) && args(warning, warningStatus, temperaDevice)",
            argNames = "warning, warningStatus, temperaDevice")

    public void logUpdateWarningStatusSuccess(Warning warning, WarningStatus warningStatus, TemperaDevice temperaDevice) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(WARNING + warning.getId() + " - TemperaDevice ID: " + temperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("warning status updated to " + warningStatus);
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when updating a Warning's status fails.
     *
     * @param warning       The warning being updated.
     * @param warningStatus The status to which the warning is being updated.
     * @param temperaDevice The device to which the warning belongs.
     * @param ex            The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.climate.WarningService.updateWarningStatus(..)) && args(warning, warningStatus, temperaDevice)",
            argNames = "warning, warningStatus, temperaDevice, ex",
            throwing = "ex")
    public void logUpdateWarningStatusError(Warning warning, WarningStatus warningStatus, TemperaDevice temperaDevice, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.UPDATE);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(WARNING + warning.getId()+ " - TemperaDevice ID: " + temperaDevice.getId());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails(ex.getMessage());
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }

    // Logging for EmailService

    /**
     * Logs the successful sending of an email.
     *
     * @param sendTo  The user receiving the email.
     * @param subject The subject of the email.
     * @param message The message of the email.
     */
    @AfterReturning(pointcut = "execution(* at.qe.skeleton.services.EmailService.sendEmail(..)) && args(sendTo, subject, message)",
            argNames = "sendTo, subject, message")
    public void logSendEmailSuccess(Userx sendTo, String subject, String message) {
        AuditLog log = new AuditLog();
        log.setAction(Action.EMAIL);
        log.setStatus(ActionStatus.SUCCESS);
        log.setAccessedResource(EMAIL + sendTo.getEmail());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Subject: " + subject);
        logRepository.save(log);
        logger.info("{}", log);
    }

    /**
     * Logs an error when sending an email fails.
     *
     * @param sendTo  The user receiving the email.
     * @param subject The subject of the email.
     * @param message The message of the email.
     * @param ex      The exception that was thrown.
     */
    @AfterThrowing(pointcut = "execution(* at.qe.skeleton.services.EmailService.sendEmail(..)) && args(sendTo, subject, message)",
            argNames = "sendTo, subject, message, ex",
            throwing = "ex")
    public void logSendEmailError(Userx sendTo, String subject, String message, Exception ex) {
        AuditLog log = new AuditLog();
        log.setAction(Action.EMAIL);
        log.setStatus(ActionStatus.ERROR);
        log.setAccessedResource(EMAIL + sendTo.getEmail());
        log.setAuthenticatedUser(getAuthenticatedUser());
        log.setTimestamp(LocalDateTime.now());
        log.setAdditionalDetails("Error: " + ex.getMessage() + " | Subject: " + subject);
        logRepository.save(log);
        String logMessage = log.toString();
        logger.info("{}", logMessage);
        errorLogger.error("{}", logMessage);
    }







    private String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.isAuthenticated() ? auth.getName() : "unauthenticated user";
    }
}