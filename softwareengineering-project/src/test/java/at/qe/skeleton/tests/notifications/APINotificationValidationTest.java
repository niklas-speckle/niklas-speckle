package at.qe.skeleton.tests.notifications;

import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.notifications.APINotificationAPBody;
import at.qe.skeleton.repositories.AccessPointRepository;
import at.qe.skeleton.rest.controllers.RestController;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.notifications.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;

/**
 * This class tests all validation mechanisms for RestController#createMessage
 */
@SpringBootTest
@WebAppConfiguration
public class APINotificationValidationTest {

    @Mock
    private AccessPointRepository accessPointRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccessPointService accessPointService;

    @InjectMocks
    private RestController restController;

    @Test
    public void testisValidAccessPointIdString() {

        Mockito.when(accessPointRepository.findAccessPointById(anyLong())).thenReturn(new AccessPoint());

        Assertions.assertTrue(accessPointService.isValidAccessPointIdString("G4T2-AP-1"));

        Assertions.assertTrue(accessPointService.isValidAccessPointIdString("G4T2-AP-1-"));

        Assertions.assertFalse(accessPointService.isValidAccessPointIdString("G4T2-AP-"));

        Assertions.assertFalse(accessPointService.isValidAccessPointIdString("G4T2-AP-1a"));

        Assertions.assertFalse(accessPointService.isValidAccessPointIdString("11"));

        Assertions.assertFalse(accessPointService.isValidAccessPointIdString("G4T1-AP-1"));

        Assertions.assertFalse(accessPointService.isValidAccessPointIdString("G4T2-TD-1"));
    }


    @Test
    public void testDoesAccessPointIDExist() {

        Mockito.when(accessPointRepository.findAccessPointById(1L)).thenReturn(new AccessPoint());

        Assertions.assertTrue(accessPointService.doesAccessPointIDExist("G4T2-AP-1"));
        Assertions.assertFalse(accessPointService.doesAccessPointIDExist("G4T2-AP-2"));
    }


    @Test
    public void testDoAccessPointAndAPINotificationMatch() {

        Mockito.doCallRealMethod().when(notificationService).doAccessPointAndAPINotificationMatch(Mockito.any(AccessPoint.class), Mockito.any(APINotificationAPBody.class));

        AccessPoint accessPoint = AccessPoint.builder().id(1L).build();

        APINotificationAPBody apiNotificationAPBody = APINotificationAPBody.builder().deviceId(1L).build();

        System.out.println("AccessPoint: " + accessPoint.getId());
        System.out.println("APINotificationAPBody: " + apiNotificationAPBody.getDeviceId());

        Assertions.assertTrue(notificationService.doAccessPointAndAPINotificationMatch(accessPoint, apiNotificationAPBody));

        apiNotificationAPBody = APINotificationAPBody.builder().deviceId(2L).build();

        Assertions.assertFalse(notificationService.doAccessPointAndAPINotificationMatch(accessPoint, apiNotificationAPBody));

    }

    @Test
    public void testIsTemperaDeviceConnectedToAccessPoint() {

        TemperaDevice temperaDevice1 = TemperaDevice.builder().id(1L).build();
        TemperaDevice temperaDevice2 = TemperaDevice.builder().id(2L).build();
        TemperaDevice temperaDevice3 = TemperaDevice.builder().id(3L).build();

        AccessPoint accessPoint = AccessPoint.builder().id(1L).temperaDevices(List.of(temperaDevice1, temperaDevice2)).build();

        Assertions.assertTrue(accessPointService.isTemperaDeviceConnectedToAccessPoint(temperaDevice1, accessPoint));
        Assertions.assertTrue(accessPointService.isTemperaDeviceConnectedToAccessPoint(temperaDevice2, accessPoint));
        Assertions.assertFalse(accessPointService.isTemperaDeviceConnectedToAccessPoint(temperaDevice3, accessPoint));

    }

    @Test
    public void testIsAccessPointAllowedToSendMessages(){
        AccessPoint accessPoint1 = AccessPoint.builder().id(1L).status(DeviceStatus.ENABLED).build();
        AccessPoint accessPoint2 = AccessPoint.builder().id(2L).status(DeviceStatus.DISABLED).build();

        Assertions.assertTrue(accessPointService.isAccessPointEnabled(accessPoint1));
        Assertions.assertFalse(accessPointService.isAccessPointEnabled(accessPoint2));
        Assertions.assertFalse(accessPointService.isAccessPointEnabled(null));
    }


}