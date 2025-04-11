package at.qe.skeleton.tests;


import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AccessPointRepository;
import at.qe.skeleton.repositories.TemperaDeviceRepository;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.TemperaDeviceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


@SpringBootTest
@WebAppConfiguration
public class AccessPointServiceTest {

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private TemperaDeviceRepository temperaDeviceRepository;

    @Autowired
    private AccessPointRepository accessPointRepository;


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})

    public void testGetAccessPointByTemperaDevice() throws EntityStillInUseException {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice = temperaDeviceRepository.save(temperaDevice);
        AccessPoint accessPoint = new AccessPoint();

        accessPoint.setTemperaDevices(new ArrayList<>(List.of(temperaDevice)));
        accessPoint = accessPointRepository.save(accessPoint);
        temperaDevice.setAccessPoint(accessPoint);
        temperaDevice = temperaDeviceRepository.save(temperaDevice);

        temperaDevice = temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId());

        AccessPoint accessPointRetrieved = accessPointRepository.findAccessPointByTemperaDevicesContains(temperaDevice);

        Assertions.assertEquals(accessPoint, accessPointRetrieved);

        //Test deletion of temperaDevice
        List<LogTemperaDevice> before = accessPoint.getLogTemperaDevices();

        Assertions.assertNull(before);
        temperaDeviceService.delete(temperaDevice);

        Assertions.assertNull(temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()));
        Assertions.assertNull(accessPointRepository.findAccessPointByTemperaDevicesContains(temperaDevice));
        Assertions.assertTrue(accessPointRetrieved.getTemperaDevices().isEmpty());
        Assertions.assertFalse(accessPointRetrieved.getLogTemperaDevices().isEmpty());
    }


//  TODO: delete(aP) does not throw Exception as expected. Change delete(aP) to check if repo returns null? Fuse delete(aP) into one method with Manuela's deleteAccessPoint(aP).
    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestDeleteAccessPoint() throws EntityStillInUseException {
        AccessPoint accessPoint = accessPointRepository.save(AccessPoint.builder().status(DeviceStatus.ENABLED).build());

        Assertions.assertDoesNotThrow(() -> accessPointService.delete(accessPoint), "Expected no exception for deleting AccessPoint");

        Assertions.assertNull(accessPointRepository.findAccessPointById(accessPoint.getId()), "AccessPoint was not deleted");


        AccessPoint accessPointWithTemperaDevice = accessPointService.save(AccessPoint.builder().status(DeviceStatus.ENABLED).build());
        TemperaDevice temperaDevice = temperaDeviceService.save(TemperaDevice.builder().status(DeviceStatus.ENABLED).build());
        accessPointWithTemperaDevice.setTemperaDevices(new ArrayList<>(List.of(temperaDevice)));
        AccessPoint accessPointWithTemperaDeviceToBeDeleted = accessPointService.save(accessPointWithTemperaDevice);
        Assertions.assertThrows(EntityStillInUseException.class, () -> accessPointService.delete(accessPointWithTemperaDeviceToBeDeleted), "Expected IllegalArgumentException for deleting null AccessPoint");

    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateAccessPoint() throws EntityStillInUseException {
        AccessPoint result = accessPointService.createAccessPoint();
        Assertions.assertNotNull(result);
        assertEquals(DeviceStatus.NOT_REGISTERED, result.getStatus());
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "GROUP_LEADER", "MANAGER"})
    public void testUnauthorizedSaveAccessPoint() {
        AccessPoint accessPoint = new AccessPoint();
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> accessPointService.save(accessPoint));
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "GROUP_LEADER", "MANAGER"})
    public void testUnauthorizedDeleteAccessPoint() {
        AccessPoint accessPoint = new AccessPoint();
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> accessPointService.delete(accessPoint));
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "GROUP_LEADER", "MANAGER"})
    public void testUnauthorizedCreateAccessPoint() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> accessPointService.createAccessPoint());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetActiveAccessPointByIdValidId() {
        AccessPoint accessPoint = accessPointService.save(AccessPoint.builder().status(DeviceStatus.ENABLED).build());
        AccessPoint accessPointLoaded = accessPointService.getActiveAccessPointById("G4T2-AP-"+accessPoint.getId());
        assertNotNull(accessPointLoaded);
        assertEquals(accessPoint, accessPointLoaded);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetActiveAccessPointByIdValidIdDisabled() {
        AccessPoint accessPoint = accessPointService.save(AccessPoint.builder().status(DeviceStatus.DISABLED).build());
        Long apID = accessPoint.getId();
        assertThrows(IllegalArgumentException.class, () -> accessPointService.getActiveAccessPointById("G4T2-AP-"+apID));
    }

    @Test
    public void testGetActiveAccessPointByIdValidIdNotInDatabase() {

        assertThatThrownBy(() -> {
            accessPointService.getActiveAccessPointById("G4T2-AP-123");
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No active AccessPoint");
    }

    @Test
    public void testGetActiveAccessPointByIdInvalidId() {
        assertThatThrownBy(() -> {
            accessPointService.getActiveAccessPointById("InvalidID");
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccessPointId must start with G4T2-");
    }

    @Test
    public void testIsAccessPointEnabled() throws EntityStillInUseException {
        AccessPoint enabledAccessPoint = new AccessPoint();
        enabledAccessPoint.setStatus(DeviceStatus.ENABLED);
        assertTrue(accessPointService.isAccessPointEnabled(enabledAccessPoint));

        AccessPoint disabledAccessPoint = new AccessPoint();
        disabledAccessPoint.setStatus(DeviceStatus.DISABLED);
        assertFalse(accessPointService.isAccessPointEnabled(disabledAccessPoint));

        AccessPoint nullAccessPoint = null;
        assertFalse(accessPointService.isAccessPointEnabled(nullAccessPoint));
    }

    @Test
    @Transactional
    public void testUpdateLastConnection(){
        AccessPoint ap = new AccessPoint();
        Assertions.assertFalse(ap.isConnected(),"Access Point should not be connected when created");

        accessPointService.updateLastConnection(ap);
        AccessPoint databaseAp = accessPointRepository.findById(ap.getId()).get();
        Assertions.assertTrue(databaseAp.getLastConnection().isAfter(LocalDateTime.now().minusSeconds(2)),"uodateConnection(ap) should update the ap.lastConnection");

        AccessPoint ap2 = new AccessPoint();
        ap2.setConnected(true);
        Assertions.assertTrue(ap2.isConnected(),"Access Point shpuld be connected after setConnected");

        accessPointService.updateLastConnection(ap2);
        databaseAp = accessPointRepository.findById(ap2.getId()).get();
        Assertions.assertTrue(databaseAp.getLastConnection().isAfter(LocalDateTime.now().minusSeconds(2)),"uodateConnection(ap2) should update the ap2.lastConnection");
        
    }

    @Test
    @Transactional
    public void testUpdateLastConnectionTime(){ //test using LocalDateTime
        AccessPoint ap = new AccessPoint();
        ap.setLastConnection(LocalDateTime.of(2024, 3, 1, 0, 0));
        Assertions.assertTrue(ap.getLastConnection().isBefore(LocalDateTime.now().minusMinutes(1)),"Last Connection was set to 01.03.2024 00:00, this should be longer ago than 1 minute!");

        accessPointService.updateLastConnection(ap);
        AccessPoint databaseAp = accessPointRepository.findById(ap.getId()).get();
        Assertions.assertFalse(databaseAp.getLastConnection().isBefore(LocalDateTime.now().minusMinutes(1)),"After calling updateLastCOnnection lastConnection should be set to now (3 lines ago) this should not be longer than 1 minute ago");       
    }
    

    @Test
    @Transactional
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "GROUP_LEADER", "MANAGER"})
    public void testSetDisconnected(){
        AccessPoint ap = new AccessPoint();
        Assertions.assertFalse(ap.isConnected(),"Access Point should not be connected when created");

        accessPointService.setDisconnected(ap);
        AccessPoint databaseAp = accessPointRepository.findById(ap.getId()).get();
        Assertions.assertFalse(databaseAp.isConnected(),"setDisconnected should set connected = false");

        AccessPoint ap2 = new AccessPoint();
        ap2.setConnected(true);
        Assertions.assertTrue(ap2.isConnected(),"Access Point shpuld be connected after setConnected");

        accessPointService.setDisconnected(ap2);
        databaseAp = accessPointRepository.findById(ap2.getId()).get();
        Assertions.assertFalse(databaseAp.isConnected(),"setDisconnected should set connected = false");
    }

    @Test
    @Transactional
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "GROUP_LEADER", "MANAGER"})
    public void testSetConnected(){
        AccessPoint ap = new AccessPoint();
        Assertions.assertFalse(ap.isConnected(),"Access Point should not be connected when created");

        ap.setConnected(true);
        Assertions.assertTrue(ap.isConnected(),"Access Point should be connected after setConnected");

        accessPointService.setDisconnected(ap);
        AccessPoint databaseAp = accessPointRepository.findById(ap.getId()).get();
        Assertions.assertFalse(databaseAp.isConnected(),"setDisconnected should set connected = false");
    }

    @Test
    @Transactional
    public void testGetNotRegisteredAPById() throws EntityStillInUseException{
        AccessPoint notRegisteredAccessPoint = new AccessPoint();
        notRegisteredAccessPoint.setStatus(DeviceStatus.NOT_REGISTERED);
        notRegisteredAccessPoint = accessPointRepository.save(notRegisteredAccessPoint);
        String idNotRegistered = notRegisteredAccessPoint.getId().toString();
        Assertions.assertEquals(notRegisteredAccessPoint, 
            accessPointService.getNotRegisteredAccessPointById("G4T2-AP-" + idNotRegistered),
            "not registered AP should be found by getNotRegisteredAccessPointById");
        Assertions.assertThrows(IllegalArgumentException.class, () -> accessPointService.getNotRegisteredAccessPointById(idNotRegistered),
            "ID should not be allowed to Satrt without G4T2-AP-");

        AccessPoint disabledAccessPoint = new AccessPoint();
        disabledAccessPoint.setStatus(DeviceStatus.DISABLED);
        Assertions.assertEquals(DeviceStatus.DISABLED, disabledAccessPoint.getStatus(),"disabledAP should be DISABLED after explicitly setting it");
        disabledAccessPoint = accessPointRepository.save(disabledAccessPoint);
        String idDisabled = disabledAccessPoint.getId().toString();
        Assertions.assertThrows(IllegalArgumentException.class,() -> accessPointService.getNotRegisteredAccessPointById("G4T2-AP-" + idDisabled),
            "when id of a disabled AP is given an IllegalArgumentException should be thrown");

        AccessPoint enabledAccessPoint = new AccessPoint();
        enabledAccessPoint.setStatus(DeviceStatus.ENABLED);
        Assertions.assertEquals(DeviceStatus.ENABLED, enabledAccessPoint.getStatus(),"enabledAccessPoint should be ENABLED after explicitly setting it");
        enabledAccessPoint = accessPointRepository.save(enabledAccessPoint);
        String idEnabled = enabledAccessPoint.getId().toString();
        Assertions.assertThrows(IllegalArgumentException.class,() -> accessPointService.getNotRegisteredAccessPointById("G4T2-AP-" + idEnabled),
            "when id of an enabled AP is given an IllegalArgumentException should be thrown");
    }

    @Test
    @Transactional
    public void testRegister() throws EntityStillInUseException {
        AccessPoint notRegisteredAccessPoint = new AccessPoint();
        notRegisteredAccessPoint.setStatus(DeviceStatus.NOT_REGISTERED);
        accessPointRepository.save(notRegisteredAccessPoint);
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, 
            accessPointRepository.findAccessPointById(notRegisteredAccessPoint.getId()).getStatus(),
            "Status should be NOT_REGISTERED after setting it so");

        accessPointService.register(notRegisteredAccessPoint);
        Assertions.assertEquals(DeviceStatus.DISABLED, 
            accessPointRepository.findAccessPointById(notRegisteredAccessPoint.getId()).getStatus(),
            "Status should have changed to DISABLED after calling register");

        AccessPoint disabledAccessPoint = new AccessPoint();
        disabledAccessPoint.setStatus(DeviceStatus.DISABLED);
        accessPointRepository.save(disabledAccessPoint);
        Assertions.assertEquals(DeviceStatus.DISABLED, 
            accessPointRepository.findAccessPointById(disabledAccessPoint.getId()).getStatus(),
            "Status should be DISABLED after setting it so");
    
        accessPointService.register(disabledAccessPoint);
        Assertions.assertEquals(DeviceStatus.DISABLED, 
            accessPointRepository.findAccessPointById(disabledAccessPoint.getId()).getStatus(),
            "Status of disabled device should not change after calling register");

        AccessPoint enabledAccessPoint = new AccessPoint();
        enabledAccessPoint.setStatus(DeviceStatus.ENABLED);
        accessPointRepository.save(enabledAccessPoint);
        Assertions.assertEquals(DeviceStatus.ENABLED, 
            accessPointRepository.findAccessPointById(enabledAccessPoint.getId()).getStatus(),
            "Status should be ENABLED after setting it so");

        accessPointService.register(enabledAccessPoint);
        Assertions.assertEquals(DeviceStatus.ENABLED, 
            accessPointRepository.findAccessPointById(enabledAccessPoint.getId()).getStatus(),
            "Status of enabled device should not change after calling register");
    }


}
