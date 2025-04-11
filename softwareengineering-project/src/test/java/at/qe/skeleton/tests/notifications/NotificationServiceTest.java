package at.qe.skeleton.tests.notifications;


import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.repositories.NotificationRepository;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class NotificationServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    AccessPointService accessPointService;

    @Autowired
    TemperaDeviceService temperaDeviceService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationService notificationService;

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetNotificationByUser() throws EntityValidationException {

        Userx user = new Userx();
        user.setUsername("notificationTestUser");
        user.setPassword("notificationTestPassword");
        user = userService.saveUser(user);

        APINotification notification1 = new APINotification();
        APINotification notification2 = new APINotification();

        Assertions.assertEquals(0, notificationService.getNotificationsByUser(user).size());

        notification1.setUser(user);
        notification2.setUser(user);

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        Assertions.assertEquals(2, notificationService.getNotificationsByUser(user).size());

    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestAddNotificationToBell() throws EntityValidationException {

        AccessPoint accessPoint = accessPointService.getAllAccessPoints().get(0);

        // admin users
        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);

        Assertions.assertFalse(admins.isEmpty());

        APINotification apiNotification = APINotificationAPBody.builder()
                .deviceId(accessPoint.getId())
                .deviceType(DeviceType.ACCESS_POINT)
                .notificationType(NotificationType.WARNING)
                .timestamp(LocalDateTime.now())
                .build();
        apiNotification.setMessage("Test Message");

        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.addNotificationToBell(accessPoint, apiNotification);

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);

        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();

        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter);

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice = temperaDeviceService.save(temperaDevice);
        Userx temperaOwner = new Userx();
        temperaOwner.setUsername("temperaOwner");
        temperaOwner.setPassword("temperaOwnerPassword");
        temperaOwner = userService.saveUser(temperaOwner);
        temperaOwner.setTemperaDevice(temperaDevice);
        temperaOwner = userService.saveUser(temperaOwner);

        APINotification apiNotificationTD = APINotificationTDBody.builder()
                .deviceId(temperaDevice.getId())
                .deviceType(DeviceType.TEMPERA_DEVICE)
                .notificationType(NotificationType.WARNING)
                .timestamp(LocalDateTime.now())
                .build();

        Userx finalUserForLambda = userService.loadUser(temperaOwner.getUsername());

        List<Integer> notificationSizePerOtherUserBefore = userService.getAllUsers().stream().filter(user -> !user.equals(finalUserForLambda) & !user.hasRole(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        notificationSizePerAdminBefore = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();
        List<Notification> notificationsBefore = notificationService.getNotificationsByUser(temperaOwner);

        notificationService.addNotificationToBell(temperaDevice, apiNotificationTD);

        List<Integer> notificationSizePerOtherUserAfter = userService.getAllUsers().stream().filter(user -> !user.equals(finalUserForLambda) & !user.hasRole(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        notificationSizePerAdminAfter = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();
        List<Notification> notificationsAfter = notificationService.getNotificationsByUser(temperaOwner);

        Assertions.assertEquals(notificationsBefore.size() + 1, notificationsAfter.size());
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter);
        Assertions.assertEquals(notificationSizePerOtherUserBefore, notificationSizePerOtherUserAfter);

    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestDeleteNotificationAdmin() throws EntityValidationException {


        AccessPoint accessPoint = accessPointService.getAllAccessPoints().get(0);

        // admin users
        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);

        Assertions.assertFalse(admins.isEmpty());

        // admin which then deletes the notification
        Userx adminProtagonist = admins.get(0);
        int adminProtagonistNotificationSizeBefore = adminProtagonist.getNotifications().size();

        APINotificationDeviceBody apiNotification = APINotificationAPBody.builder()
                .deviceId(accessPoint.getId())
                .deviceType(DeviceType.ACCESS_POINT)
                .notificationType(NotificationType.WARNING)
                .timestamp(LocalDateTime.now())
                .build();
        apiNotification.setMessage("Test Message");

        notificationService.addNotificationToBell(accessPoint, apiNotification);

        List<Integer> notificationSizePerOtherAdminBefore = admins.stream().filter(userx -> !userx.equals(adminProtagonist)).map(user -> user.getNotifications().size()).toList();

        Userx adminProtagonistAfterAddNotification = userService.loadUser(adminProtagonist.getUsername());

        Assertions.assertEquals(adminProtagonistNotificationSizeBefore + 1,
                adminProtagonistAfterAddNotification.getNotifications().size(),
                "Admin did not receive AP message"
        );

        List<Notification> adminNotifications = adminProtagonistAfterAddNotification.getNotifications();
        notificationService.deleteNotification(adminNotifications.get(adminNotifications.size() - 1));

        Assertions.assertEquals(adminProtagonistNotificationSizeBefore,
                userService.loadUser(adminProtagonist.getUsername()).getNotifications().size(),
                "Deletion of notification did not work"
        );

        List<Integer> notificationSizePerOtherAdminAfter = admins.stream().filter(userx -> !userx.equals(adminProtagonist)).map(user -> user.getNotifications().size()).toList();

        Assertions.assertEquals(notificationSizePerOtherAdminBefore, notificationSizePerOtherAdminAfter, "Deletion of notification did affect other admins notifications.");


    }


    @Test
    @Transactional
    @WithMockUser(username = "temperaOwner", authorities = {"ADMINISTRATOR"})
    public void integrationTestDeleteNotificationUser() throws EntityValidationException {

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice = temperaDeviceService.save(temperaDevice);
        Userx temperaOwner = new Userx();
        temperaOwner.setUsername("temperaOwner");
        temperaOwner.setPassword("temperaOwnerPassword");
        temperaOwner = userService.saveUser(temperaOwner);
        temperaOwner.setTemperaDevice(temperaDevice);
        temperaOwner = userService.saveUser(temperaOwner);

        APINotification apiNotificationTD = APINotificationTDBody.builder()
                .deviceId(temperaDevice.getId())
                .deviceType(DeviceType.TEMPERA_DEVICE)
                .notificationType(NotificationType.WARNING)
                .timestamp(LocalDateTime.now())
                .build();

        Userx finalUserForLambda = userService.loadUser(temperaOwner.getUsername());
        List<Integer> notificationSizePerOtherUserBefore = userService.getAllUsers().stream().filter(user -> !user.equals(finalUserForLambda) & !user.hasRole(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        int notificationsSizeBefore = notificationService.getNotificationsByUser(temperaOwner).size();
        List<Integer> notificationSizePerAdminBefore = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();

        notificationService.addNotificationToBell(temperaDevice, apiNotificationTD);

        List<Integer> notificationSizePerOtherUserAfter = userService.getAllUsers().stream().filter(user -> !user.equals(finalUserForLambda) & !user.hasRole(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        List<Notification> notificationsAfter = notificationService.getNotificationsByUser(temperaOwner);
        List<Integer> notificationSizePerAdminAfter = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();

        Assertions.assertEquals(notificationsSizeBefore + 1, notificationsAfter.size());
        Assertions.assertEquals(notificationSizePerOtherUserBefore, notificationSizePerOtherUserAfter);
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter);

        Notification notificationToDelete = notificationsAfter.get(notificationsAfter.size() - 1);

        notificationService.deleteNotification(notificationToDelete);

        List<Notification> notificationsAfterDeletion = notificationService.getNotificationsByUser(temperaOwner);
        List<Integer> notificationSizePerOtherUserAfterDeletion = userService.getAllUsers().stream().filter(user -> !user.equals(finalUserForLambda) & !user.hasRole(UserxRole.ADMINISTRATOR)).map(user -> user.getNotifications().size()).toList();
        List<Integer> notificationSizePerAdminAfterDeletion = userService.getUsersByRole(UserxRole.ADMINISTRATOR).stream().map(user -> user.getNotifications().size()).toList();

        Assertions.assertEquals(notificationsSizeBefore, notificationsAfterDeletion.size());
        Assertions.assertEquals(notificationSizePerOtherUserBefore, notificationSizePerOtherUserAfterDeletion);
        Assertions.assertEquals(notificationSizePerAdminAfter, notificationSizePerAdminAfterDeletion);
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void unauthorizedNotificationDeleteTest() throws EntityValidationException {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice = temperaDeviceService.save(temperaDevice);
        Userx temperaOwner = new Userx();
        temperaOwner.setUsername("temperaOwner");
        temperaOwner.setPassword("temperaOwnerPassword");
        temperaOwner = userService.saveUser(temperaOwner);
        temperaOwner.setTemperaDevice(temperaDevice);
        temperaOwner = userService.saveUser(temperaOwner);

        APINotification apiNotificationTD = APINotificationTDBody.builder()
                .deviceId(temperaDevice.getId())
                .deviceType(DeviceType.TEMPERA_DEVICE)
                .notificationType(NotificationType.WARNING)
                .timestamp(LocalDateTime.now())
                .build();

        notificationService.addNotificationToBell(temperaDevice, apiNotificationTD);
        List<Notification> temperaOwnerNotification = userService.loadUser(temperaOwner.getUsername()).getNotifications();

        Notification notificationToDelete = temperaOwnerNotification.get(0);

        Assertions.assertThrows(AccessDeniedException.class, () -> {
            notificationService.deleteNotification(notificationToDelete);
        });
    }

    @Test
    @Transactional
    public void testDeleteNotificationExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            notificationService.deleteNotification(null);
        });

        APINotification apiNotification = new APINotification();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            notificationService.deleteNotification(apiNotification);
        });
    }


    @Test
    @Transactional
    public void testCheckConnectionOfAccessPointsEnabledNotConnected() throws EntityValidationException {
        List<AccessPoint> listApDatabase = accessPointService.getAllAccessPoints();
        for(AccessPoint ap : listApDatabase){
            Assertions.assertFalse(ap.isConnected(),"AP that are already in Database should be not connected");
            //Assertions.assertEquals(DeviceStatus.ENABLED, ap.getStatus(), "AP that are in Database should all be ENABLED");
        }

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints();

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore, notificationSizePerAdminAfter,"AP that are already set disconnected should not create messages");
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCheckConnectionOfAccessPointsWrongStatus() throws EntityStillInUseException, EntityValidationException {
        List<AccessPoint> listApDatabase = accessPointService.getAllAccessPoints();
        for(AccessPoint ap : listApDatabase){ //check that these do not influence the test below (as checked in testCheckConnectionOfAccessPointsEnabledNotConnected)
            Assertions.assertFalse(ap.isConnected(),"AP that are already in Database should be not connected");
            //Assertions.assertEquals(DeviceStatus.ENABLED, ap.getStatus(), "AP that are in Database should all be ENABLED");
        }

        AccessPoint apDisabled = new AccessPoint();
        apDisabled.setStatus(DeviceStatus.DISABLED);
        accessPointService.save(apDisabled);
        AccessPoint apNotRegistered = new AccessPoint();
        apNotRegistered.setStatus(DeviceStatus.NOT_REGISTERED);
        accessPointService.save(apNotRegistered);

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints();

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore, notificationSizePerAdminAfter,"AP with staus of NOT_REGISTERED or DISABLED should not create messages");
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCheckConnectionOfAccessPointsEnabledConnectedOld() throws EntityStillInUseException, EntityValidationException {
        List<AccessPoint> listApDatabase = accessPointService.getAllAccessPoints();
        for(AccessPoint ap : listApDatabase){
            Assertions.assertFalse(ap.isConnected(),"AP that are already in Database should be not connected");
        }


        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints();

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore, notificationSizePerAdminAfter,"Accesspoints already in Repository should not create messages");

        // new AP connected = true, lastConnection = 01.03.2024
        AccessPoint apOld = new AccessPoint(); //lastConnection is long ago - should create message
        apOld.setStatus(DeviceStatus.ENABLED);
        apOld.setConnected(true);
        apOld.setLastConnection(LocalDateTime.of(2024, 3, 1, 0, 0));
        accessPointService.save(apOld);
        

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints();

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter,"Enabled and connected AP with old timestamp (01.03.2024) should create message for al admins");
        
        listApDatabase = accessPointService.getAllAccessPoints(); // oldAp should also be disconnected by now
        for(AccessPoint ap : listApDatabase){
            Assertions.assertFalse(ap.isConnected(),"AP that are already in Database should be not connected");
        }

        // new AP connected = true, lastConnection = 3 minutes ago
        AccessPoint ap3min = new AccessPoint(); //lastConnection is long ago - should create message
        ap3min.setStatus(DeviceStatus.ENABLED);
        ap3min.setConnected(true);
        ap3min.setLastConnection(LocalDateTime.now().minusMinutes(3));
        accessPointService.save(ap3min);
        

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints(); // apOld should already be disconnected

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore.stream().map(i -> i + 1).toList(), notificationSizePerAdminAfter,"Enabled and connected AP with timestamp  that is 3 min old should create message for al admins");
        
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCheckConnectionOfAccessPointsEnabledConnectedNew() throws EntityStillInUseException, EntityValidationException {
        List<AccessPoint> listApDatabase = accessPointService.getAllAccessPoints();
        for(AccessPoint ap : listApDatabase){
            Assertions.assertFalse(ap.isConnected(),"AP that are already in Database should be not connected");
        }

        List<Userx> admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        List<Integer> notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints();

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        List<Integer> notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore, notificationSizePerAdminAfter,"Accesspoints already in Repository should not create messages");

        // new AP connected = true, lastConnection = 1 min ago
        AccessPoint ap1min = new AccessPoint(); //should ot create message
        ap1min.setStatus(DeviceStatus.ENABLED);
        ap1min.setConnected(true);
        ap1min.setLastConnection(LocalDateTime.now().minusMinutes(1));
        accessPointService.save(ap1min);
        

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR);
        Assertions.assertNotNull(admins,"list of administrators should not be null");
        notificationSizePerAdminBefore = admins.stream().map(user -> user.getNotifications().size()).toList();

        notificationService.checkConnectionOfAccessPoints();

        admins = userService.getUsersByRole(UserxRole.ADMINISTRATOR); // new database call necessary to get new notifications
        notificationSizePerAdminAfter = admins.stream().map(user -> user.getNotifications().size()).toList();
        Assertions.assertEquals(notificationSizePerAdminBefore, notificationSizePerAdminAfter,"Enabled and connected AP with old timestamp (01.03.2024) should create message for al admins");
        
    }


}
