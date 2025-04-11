package at.qe.skeleton.tests;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.LogTemperaDevice;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.UserxRole;
import at.qe.skeleton.rest.controllers.RestController;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.UserService;

@SpringBootTest
public class RestControllerRegistrationAndUpdateTest {

    @Autowired
    private RestController restController;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private UserService userService;

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testUpdateConnectionConnectedAP() throws EntityValidationException, EntityStillInUseException{
        AccessPoint connectedAP = new AccessPoint();
        connectedAP.setStatus(DeviceStatus.ENABLED);
        connectedAP.setConnected(true);
        connectedAP.setLogTemperaDevices(new ArrayList<LogTemperaDevice>());
        LocalDateTime previousTime = LocalDateTime.of(2024,3,1,0,0);
        connectedAP.setLastConnection(previousTime);

        Assertions.assertEquals(previousTime, connectedAP.getLastConnection(), "lastConnection should be previousTime as set 2 lines above");
        accessPointService.save(connectedAP);

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        restController.getLogTemperaDeviceByAccessPointId("G4T2-AP-"+connectedAP.getId().toString()); // should call updateConnection internally

        AccessPoint updatedAP = accessPointService.getAccessPointById(connectedAP.getId());
        Assertions.assertNotEquals(previousTime, updatedAP.getLastConnection(), "after calling updateConnection (in getLogTmperaDeviceByAccessPointId) lastConnection should have changed");
        Assertions.assertTrue(updatedAP.getLastConnection().isAfter(previousTime), "lastConnection (after update) should be newer than previousTime");

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore, notificationSizePerAdminAfter,"already connected AP should not create a message");

    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testUpdateConnectionNotConnectedAP() throws EntityValidationException, EntityStillInUseException{
        AccessPoint notConnectedAP = new AccessPoint();
        notConnectedAP.setStatus(DeviceStatus.ENABLED);
        notConnectedAP.setConnected(false);
        notConnectedAP.setLogTemperaDevices(new ArrayList<LogTemperaDevice>());
        LocalDateTime previousTime = LocalDateTime.of(2024,3,1,0,0);
        notConnectedAP.setLastConnection(previousTime);

        Assertions.assertEquals(previousTime, notConnectedAP.getLastConnection(), "lastConnection should be previousTime as set 2 lines above");
        Assertions.assertFalse(notConnectedAP.isConnected());
        accessPointService.save(notConnectedAP);

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        restController.getLogTemperaDeviceByAccessPointId("G4T2-AP-"+notConnectedAP.getId().toString()); // should call updateConnection internally

        AccessPoint updatedAP = accessPointService.getAccessPointById(notConnectedAP.getId());
        Assertions.assertNotEquals(previousTime, updatedAP.getLastConnection(), "after calling updateConnection (in getLogTmperaDeviceByAccessPointId) lastConnection should have changed");
        Assertions.assertTrue(updatedAP.getLastConnection().isAfter(previousTime), "lastConnection (after update) should be newer than previousTime");

        Assertions.assertTrue(updatedAP.isConnected(), "AP should be connected after calling updateConnection");

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter,"not connected AP should create a new message (connected AP successfully... something like that)");

    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testNotRegisteredAP() throws EntityStillInUseException, IdNotFoundException, EntityValidationException{
        AccessPoint notRegisterdAccessPoint = accessPointService.createAccessPoint();
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, notRegisterdAccessPoint.getStatus(),"device status should be not_registered after creation through accessPOintService");
        accessPointService.save(notRegisterdAccessPoint); //save in database

        AccessPoint accessPointBefore = accessPointService.getAccessPointById(notRegisterdAccessPoint.getId());
        Assertions.assertNotNull(accessPointBefore,"AP should be saved/retreivable in database and not null");
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, accessPointBefore.getStatus(),"AP should still be not_registered after saving and retreiving it from database");

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        APINotificationDTO dto = new APINotificationDTO(null, null, null, null, null);
        Assertions.assertEquals(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("AccessPoint has been registered, but is not yet enabled"), 
            restController.createMessage(dto, "G4T2-AP-" + notRegisterdAccessPoint.getId().toString()),
            "When a not_registered AP is discovered in createMessage, the method should return a UNAUTHORIZED with a message about the device being registered but not yet enabled");
        
        AccessPoint accessPointAfter = accessPointService.getAccessPointById(notRegisterdAccessPoint.getId());
        Assertions.assertNotNull(accessPointAfter, "AP should retreivable from database after changes");
        Assertions.assertEquals(DeviceStatus.DISABLED, accessPointAfter.getStatus(), "the status of the AP should have been changed to DISABLED from NOT_REGISTERED");

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter,"registration of a NOT_REGISTERED AP should create a message for all admins");

    }

    
    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDisabledAP() throws EntityStillInUseException, IdNotFoundException, EntityValidationException{
        AccessPoint disabledAccessPoint = accessPointService.createAccessPoint();
        disabledAccessPoint.setStatus(DeviceStatus.DISABLED);
        Assertions.assertEquals(DeviceStatus.DISABLED, disabledAccessPoint.getStatus(),"device status should be disabled after explicitly setting it so");
        accessPointService.save(disabledAccessPoint); //save in database

        AccessPoint accessPointBefore = accessPointService.getAccessPointById(disabledAccessPoint.getId());
        Assertions.assertNotNull(accessPointBefore,"AP should be saved/retreivable in database and not null");
        Assertions.assertEquals(DeviceStatus.DISABLED, accessPointBefore.getStatus(),"AP should still be disabled after saving and retreiving it from database");

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        APINotificationDTO dto = new APINotificationDTO(null, null, null, null, null);
        restController.createMessage(dto, "G4T2-AP-" + disabledAccessPoint.getId().toString());
        
        AccessPoint accessPointAfter = accessPointService.getAccessPointById(disabledAccessPoint.getId());
        Assertions.assertNotNull(accessPointAfter, "AP should retreivable from database after method call");
        Assertions.assertEquals(DeviceStatus.DISABLED, accessPointAfter.getStatus(), "the status of the AP should not have changed from disabled");

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i).toList(), notificationSizePerAdminAfter,"no message schould be created for disabled AP");

    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testEnabledAP() throws EntityStillInUseException, IdNotFoundException, EntityValidationException{
        AccessPoint enabledAccessPoint = accessPointService.createAccessPoint();
        enabledAccessPoint.setStatus(DeviceStatus.ENABLED);
        enabledAccessPoint.setConnected(true); //AP should be connected so that no new Message (from connection) is enabled
        Assertions.assertEquals(DeviceStatus.ENABLED, enabledAccessPoint.getStatus(),"device status should be enabled after explicitly setting it so");
        accessPointService.save(enabledAccessPoint); //save in database

        AccessPoint accessPointBefore = accessPointService.getAccessPointById(enabledAccessPoint.getId());
        Assertions.assertNotNull(accessPointBefore,"AP should be saved/retreivable in database and not null");
        Assertions.assertEquals(DeviceStatus.ENABLED, accessPointBefore.getStatus(),"AP should still be enabled after saving and retreiving it from database");
        Assertions.assertTrue(accessPointBefore.isConnected(),"Should still be true after explicitly setting it true before save in database");

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        APINotificationDTO dto = new APINotificationDTO(null, null, null, null, null);
        restController.createMessage(dto, "G4T2-AP-" + enabledAccessPoint.getId().toString());
        
        AccessPoint accessPointAfter = accessPointService.getAccessPointById(enabledAccessPoint.getId());
        Assertions.assertNotNull(accessPointAfter, "AP should retreivable from database after method call");
        Assertions.assertEquals(DeviceStatus.ENABLED, accessPointAfter.getStatus(), "the status of the AP should not have changed from enabled");

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i).toList(), notificationSizePerAdminAfter,"no message schould be created for enabled AP");

    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testNotRegisteredTD() throws EntityStillInUseException, IdNotFoundException, EntityValidationException{
        AccessPoint accessPoint = accessPointService.createAccessPoint();
        accessPoint.setStatus(DeviceStatus.ENABLED);
        accessPoint.setConnected(true);

        TemperaDevice notRegisteredTemperaDevice = temperaDeviceService.createTemperaDevice();
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, notRegisteredTemperaDevice.getStatus(), "newly generated TD (through temperaDeviceService) should be NOT_REGISTERED");
        temperaDeviceService.save(notRegisteredTemperaDevice);

        List<TemperaDevice> listTD = new ArrayList<>();
        listTD.add(notRegisteredTemperaDevice);
        accessPoint.setTemperaDevices(listTD);
        accessPointService.save(accessPoint);

        AccessPoint savedAccessPoint = accessPointService.getAccessPointById(accessPoint.getId());
        Assertions.assertNotNull(savedAccessPoint,"AP should be reachable through database");
        Assertions.assertEquals(listTD, savedAccessPoint.getTemperaDevices(), "the given list should still be there after saving/retreiving the AP from the database");

        TemperaDevice temperaDeviceBefore = temperaDeviceService.findTemperaDeviceById(notRegisteredTemperaDevice.getId());
        Assertions.assertNotNull(temperaDeviceBefore,"TD should be found in DB");
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, temperaDeviceBefore.getStatus(),"The satus of the TD should still be NOT_REGISTERED after saving it in DB");
        Assertions.assertTrue(savedAccessPoint.getTemperaDevices().contains(temperaDeviceBefore),"TD should be an element of the tempera devices of the AP");

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();


        APINotificationDTO dto = new APINotificationDTO(null, "TD", temperaDeviceBefore.getId(), "1", null);
        Assertions.assertEquals(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("TemperaDevice has been registered, but is not yet enabled"), 
            restController.createMessage(dto, "G4T2-AP-" + savedAccessPoint.getId()));

        TemperaDevice temperaDeviceAfter = temperaDeviceService.findTemperaDeviceById(notRegisteredTemperaDevice.getId());
        Assertions.assertNotNull(temperaDeviceAfter,"TD should be found in DB");
        Assertions.assertEquals(DeviceStatus.DISABLED, temperaDeviceAfter.getStatus(),"The satus of the TD should have changed to DISABLED from NOT_REGISTERED");
       
        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter,"registration of a NOT_REGISTERED TD should create a message for all admins");

    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDisabledTD() throws EntityStillInUseException, IdNotFoundException, EntityValidationException{
        AccessPoint accessPoint = accessPointService.createAccessPoint();
        accessPoint.setStatus(DeviceStatus.ENABLED);
        accessPoint.setConnected(true);

        TemperaDevice disabledTemperaDevice = temperaDeviceService.createTemperaDevice();
        disabledTemperaDevice.setStatus(DeviceStatus.DISABLED);
        Assertions.assertEquals(DeviceStatus.DISABLED, disabledTemperaDevice.getStatus(), "device status should be DISABLED after explicitly setting it so");
        temperaDeviceService.save(disabledTemperaDevice);

        List<TemperaDevice> listTD = new ArrayList<>();
        listTD.add(disabledTemperaDevice);
        accessPoint.setTemperaDevices(listTD);
        accessPointService.save(accessPoint);

        AccessPoint savedAccessPoint = accessPointService.getAccessPointById(accessPoint.getId());
        Assertions.assertNotNull(savedAccessPoint,"AP should be reachable through database");
        Assertions.assertEquals(listTD, savedAccessPoint.getTemperaDevices(), "the given list should still be there after saving/retreiving the AP from the database");

        TemperaDevice temperaDeviceBefore = temperaDeviceService.findTemperaDeviceById(disabledTemperaDevice.getId());
        Assertions.assertNotNull(temperaDeviceBefore,"TD should be found in DB");
        Assertions.assertEquals(DeviceStatus.DISABLED, temperaDeviceBefore.getStatus(),"The satus of the TD should still be DISABLED after saving it in DB");
        Assertions.assertTrue(savedAccessPoint.getTemperaDevices().contains(temperaDeviceBefore),"TD should be an element of the tempera devices of the AP");

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();


        APINotificationDTO dto = new APINotificationDTO(null, "TD", temperaDeviceBefore.getId(), null, null);
        restController.createMessage(dto, "G4T2-AP-" + savedAccessPoint.getId());

        TemperaDevice temperaDeviceAfter = temperaDeviceService.findTemperaDeviceById(disabledTemperaDevice.getId());
        Assertions.assertNotNull(temperaDeviceAfter,"TD should be found in DB");
        Assertions.assertEquals(DeviceStatus.DISABLED, temperaDeviceAfter.getStatus(),"The satus of the TD should not have changed and still be DISABLED");
       
        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i).toList(), notificationSizePerAdminAfter,"no message should be created for disabled TD");

    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testEnabledTD() throws EntityStillInUseException, IdNotFoundException, EntityValidationException{
        AccessPoint accessPoint = accessPointService.createAccessPoint();
        accessPoint.setStatus(DeviceStatus.ENABLED);
        accessPoint.setConnected(true);

        TemperaDevice enabledTemperaDevice = temperaDeviceService.createTemperaDevice();
        enabledTemperaDevice.setStatus(DeviceStatus.ENABLED);
        Assertions.assertEquals(DeviceStatus.ENABLED, enabledTemperaDevice.getStatus(), "device status should be ENABLED after explicitly setting it so");
        temperaDeviceService.save(enabledTemperaDevice);

        List<TemperaDevice> listTD = new ArrayList<>();
        listTD.add(enabledTemperaDevice);
        accessPoint.setTemperaDevices(listTD);
        accessPointService.save(accessPoint);

        AccessPoint savedAccessPoint = accessPointService.getAccessPointById(accessPoint.getId());
        Assertions.assertNotNull(savedAccessPoint,"AP should be reachable through database");
        Assertions.assertEquals(listTD, savedAccessPoint.getTemperaDevices(), "the given list should still be there after saving/retreiving the AP from the database");

        TemperaDevice temperaDeviceBefore = temperaDeviceService.findTemperaDeviceById(enabledTemperaDevice.getId());
        Assertions.assertNotNull(temperaDeviceBefore,"TD should be found in DB");
        Assertions.assertEquals(DeviceStatus.ENABLED, temperaDeviceBefore.getStatus(),"The satus of the TD should still be ENABLED after saving it in DB");
        Assertions.assertTrue(savedAccessPoint.getTemperaDevices().contains(temperaDeviceBefore),"TD should be an element of the tempera devices of the AP");

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();


        APINotificationDTO dto = new APINotificationDTO(null, "TD", temperaDeviceBefore.getId(), null, null);
        restController.createMessage(dto, "G4T2-AP-" + savedAccessPoint.getId());

        TemperaDevice temperaDeviceAfter = temperaDeviceService.findTemperaDeviceById(enabledTemperaDevice.getId());
        Assertions.assertNotNull(temperaDeviceAfter,"TD should be found in DB");
        Assertions.assertEquals(DeviceStatus.ENABLED, temperaDeviceAfter.getStatus(),"The satus of the TD should not have changed and still be ENABLED");
       
        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i).toList(), notificationSizePerAdminAfter,"no message should be created for enabled TD");

    }



}
