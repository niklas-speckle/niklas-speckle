package at.qe.skeleton.tests.notifications;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.repositories.NotificationRepository;
import at.qe.skeleton.rest.controllers.RestController;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import at.qe.skeleton.rest.mapper.APINotificationDeviceBodyDTOMapper;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@WebAppConfiguration
public class APINotificationTest {

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    APINotificationDeviceBodyDTOMapper apiNotificationDeviceBodyDTOMapper;

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    TemperaDeviceService temperaDeviceService;

    @Autowired
    UserService userService;

    @Autowired
    RestController restController;


    // TODO: fix or delete
    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestCreateAPMessage() throws EntityValidationException, IdNotFoundException, EntityStillInUseException {

        Userx newAdmin = Userx.builder().username("newAdmin").password("password").roles(new ArrayList<>(List.of(UserxRole.ADMINISTRATOR))).notifications(new ArrayList<Notification>()).build();
        newAdmin = userService.saveUser(newAdmin);

        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).build();
        accessPoint = accessPointService.save(accessPoint);

        APINotificationDTO apiNotificationDTOAP = APINotificationDTO.builder()
                .deviceId(accessPoint.getId())
                .deviceType("AP")
                //Warning
                .notificationType("1")
                .message("message")
                .timestamp(null)
                .build();


        // each admin should receive one notification
        List<Integer> notificationSizePerAdminBefore = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();
        List<Notification> allNotificationsBefore = notificationRepository.findAll();

        HttpStatusCode responseCode = restController.createMessage(apiNotificationDTOAP, "G4T2-AP-" + accessPoint.getId()).getStatusCode();

        Assertions.assertEquals(HttpStatus.OK, responseCode);

        List<Integer> notificationSizePerAdminAfter = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();

        List<Notification> allNotificationsAfter = notificationRepository.findAll();

        int numAdmins = userService.getUsersByRole(UserxRole.ADMINISTRATOR).size();

        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter);
        Assertions.assertEquals(allNotificationsBefore.size() + numAdmins, allNotificationsAfter.size());
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestCreateTDMessage() throws EntityValidationException, IdNotFoundException, EntityStillInUseException {

        Userx newAdmin = Userx.builder().username("newAdmin").password("password").roles(new ArrayList<>(List.of(UserxRole.ADMINISTRATOR))).notifications(new ArrayList<Notification>()).build();
        newAdmin = userService.saveUser(newAdmin);

        TemperaDevice temperaDevice = temperaDeviceService.createTemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice = temperaDeviceService.save(temperaDevice);

        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).build();
        accessPoint.setTemperaDevices(new ArrayList<>(List.of(temperaDevice)));
        accessPoint = accessPointService.save(accessPoint);

        temperaDevice.setAccessPoint(accessPoint);
        temperaDevice = temperaDeviceService.save(temperaDevice);

        Userx userx = Userx.builder().username("userxForRestTest").password("password").roles(new ArrayList<>(List.of(UserxRole.EMPLOYEE))).notifications(new ArrayList<Notification>()).build();
        userx.setTemperaDevice(temperaDevice);
        userx = userService.saveUser(userx);

        APINotificationDTO apiNotificationDTOTD = APINotificationDTO.builder()
                .deviceId(temperaDevice.getId())
                .deviceType("TD")
                //Warning
                .notificationType("2")
                .message("message")
                .timestamp(null)
                .build();

        // should not receive notification
        List<Integer> notificationSizePerOtherUserxBefore = userService.getAllUsers().stream().filter(u -> !u.getUsername().equals("userxForRestTest") & !u.getRoles().contains(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        // should receive notification
        int notificationForProtagonistBeforeSize = userService.loadUser(userx.getUsername()).getNotifications().size();
        // should receive notification
        List<Integer> notificationSizePerAdminBefore = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();

        int allNotificationsBeforeSize = notificationRepository.findAll().size();

        HttpStatusCode responseStatus = restController.createMessage(apiNotificationDTOTD, "G4T2-AP-" + accessPoint.getId()).getStatusCode();

        Assertions.assertEquals(HttpStatus.OK, responseStatus);

        List<Integer> notificationSizePerOtherUserxAfter = userService.getAllUsers().stream().filter(u -> !u.getUsername().equals("userxForRestTest") & !u.getRoles().contains(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        List<Notification> notificationForProtagonistAfter = userService.loadUser(userx.getUsername()).getNotifications();
        List<Integer> notificationSizePerAdminAfter = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();

        List<Notification> allNotificationsAfter = notificationRepository.findAll();

        Assertions.assertEquals(notificationSizePerOtherUserxBefore, notificationSizePerOtherUserxAfter);

        Assertions.assertEquals(notificationForProtagonistBeforeSize + 1, notificationForProtagonistAfter.size());

        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter);

        int numAdmins = userService.getUsersByRole(UserxRole.ADMINISTRATOR).size();

        Assertions.assertEquals(allNotificationsBeforeSize + 1 + numAdmins, allNotificationsAfter.size());

    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMessageInvalidID() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {

        APINotificationDTO apiNotificationDTOAP = APINotificationDTO.builder()
                .deviceId(1001L)
                .deviceType("AP")
                //Warning
                .notificationType("1")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOAP, "G4T2-TD-1002");

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        // only AP are allowed to send messages
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        numAllNotificationsBefore = notificationRepository.findAll().size();

        response = restController.createMessage(apiNotificationDTOAP, "G4T2-AP-1001");

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMessageAPNotFound() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {
        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).id(0L).build();

        APINotificationDTO apiNotificationDTOAP = APINotificationDTO.builder()
                .deviceId(accessPoint.getId())
                .deviceType("AP")
                .notificationType("1")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOAP, "G4T2-AP-" + accessPoint.getId());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        System.out.println("Response Body: ");
        System.out.println(response.getBody());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMessageDisabledAP() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {
        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.DISABLED).connected(true).build();
        accessPoint = accessPointService.save(accessPoint);

        APINotificationDTO apiNotificationDTOAP = APINotificationDTO.builder()
                .deviceId(accessPoint.getId())
                .deviceType("AP")
                .notificationType("1")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOAP, "G4T2-AP-" + accessPoint.getId());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMessageTDdisabled() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {
        TemperaDevice temperaDevice = temperaDeviceService.createTemperaDevice();
        temperaDevice.setStatus(DeviceStatus.DISABLED);
        temperaDevice = temperaDeviceService.save(temperaDevice);

        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).build();
        temperaDevice.setAccessPoint(accessPoint);
        temperaDevice = temperaDeviceService.save(temperaDevice);
        accessPoint.setTemperaDevices(new ArrayList<>(List.of(temperaDevice)));
        accessPoint = accessPointService.save(accessPoint);

        APINotificationDTO apiNotificationDTOTD = APINotificationDTO.builder()
                .deviceId(temperaDevice.getId())
                .deviceType("TD")
                .notificationType("2")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOTD, "G4T2-AP-" + accessPoint.getId());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMessageTDAndAPMissmatch() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {
        TemperaDevice temperaDevice = temperaDeviceService.createTemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice = temperaDeviceService.save(temperaDevice);

        Userx userx = Userx.builder().username("userxForRestTest").password("password").roles(new ArrayList<>(List.of(UserxRole.EMPLOYEE))).build();
        userx.setTemperaDevice(temperaDevice);
        userx = userService.saveUser(userx);

        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).temperaDevices(new ArrayList<TemperaDevice>()).build();
        accessPoint = accessPointService.save(accessPoint);

        APINotificationDTO apiNotificationDTOTD = APINotificationDTO.builder()
                .deviceId(temperaDevice.getId())
                .deviceType("TD")
                .notificationType("2")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOTD, "G4T2-AP-" + accessPoint.getId());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMesssageInvalidAPINotificationDTO_deviceType() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {

        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).temperaDevices(new ArrayList<TemperaDevice>()).build();
        accessPoint = accessPointService.save(accessPoint);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, restController.createMessage(null, "G4T2-AP-" + accessPoint.getId()).getStatusCode());

        APINotificationDTO apiNotificationDTOInvalidDeviceType = APINotificationDTO.builder()
                .deviceId(accessPoint.getId())
                // invalid
                .deviceType("XP")
                .notificationType("1")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOInvalidDeviceType, "G4T2-AP-" + accessPoint.getId());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateMesssageInvalidAPINotificationDTO_notificationType() throws EntityValidationException, EntityStillInUseException, IdNotFoundException {

        AccessPoint accessPoint = AccessPoint.builder().status(DeviceStatus.ENABLED).connected(true).temperaDevices(new ArrayList<TemperaDevice>()).build();
        accessPoint = accessPointService.save(accessPoint);

        APINotificationDTO apiNotificationDTOInvalidNotificationType = APINotificationDTO.builder()
                .deviceId(accessPoint.getId())
                .deviceType("AP")
                // invalid
                .notificationType("X")
                .message("message")
                .timestamp(null)
                .build();

        int numAllNotificationsBefore = notificationRepository.findAll().size();

        ResponseEntity<String> response = restController.createMessage(apiNotificationDTOInvalidNotificationType, "G4T2-AP-" + accessPoint.getId());

        int numAllNotificationsAfter = notificationRepository.findAll().size();

        Assertions.assertEquals(numAllNotificationsBefore, numAllNotificationsAfter);

        System.out.println("Response Body: ");
        System.out.println(response.getBody());

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }







}
