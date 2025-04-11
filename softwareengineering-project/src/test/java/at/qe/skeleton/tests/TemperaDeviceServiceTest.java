package at.qe.skeleton.tests;


import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.TemperaDeviceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WebAppConfiguration
public class TemperaDeviceServiceTest {

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private TemperaDeviceRepository temperaDeviceRepository;

    @Autowired
    private UserxRepository userxRepository;

    @Autowired
    private AccessPointRepository accessPointRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private LogTemperaDeviceRepository logTemperaDeviceRepository;

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetAllTemperaDevices() {
        List<TemperaDevice> temperaDevicesFromDB = temperaDeviceRepository.findAll();
        int totalNumberOfDevices = temperaDevicesFromDB.size();

        List<TemperaDevice> temperaDevicesViaService = temperaDeviceService.getAllTemperaDevices();
        assertEquals(totalNumberOfDevices, temperaDevicesViaService.size());
        assertTrue(temperaDevicesViaService.containsAll(temperaDevicesFromDB));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateTemperaDevice() {
        TemperaDevice temperaDevice = temperaDeviceService.createTemperaDevice();

        Assertions.assertNotNull(temperaDevice, "Returned TemperaDevice is null");
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, temperaDevice.getStatus(), "Expected TemperaDevice Status to be NOT_REGISTERED");

        Assertions.assertNotNull(temperaDevice.getSensors(), "Sensors list is null");
        Assertions.assertEquals(SensorType.values().length, temperaDevice.getSensors().size(), "Expected number of sensors does not match");

        List<SensorType> listOfSensorTypes = new ArrayList<>(Arrays.asList(SensorType.values()));

        for (Sensor sensor : temperaDevice.getSensors()) {
            Assertions.assertNotNull(sensor, "Sensor is null");
            Assertions.assertTrue(listOfSensorTypes.contains(sensor.getSensorType()), "SensorType not found in list of SensorTypes");
            listOfSensorTypes.remove(sensor.getSensorType());
        }

        Assertions.assertEquals(0, listOfSensorTypes.size(), "Not all SensorTypes were found in the list of Sensors");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testSaveTemperaDevice() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.DISABLED);

        TemperaDevice savedTemperaDevice = temperaDeviceService.save(temperaDevice);

        Assertions.assertNotNull(savedTemperaDevice, "Saved TemperaDevice is null");
        Assertions.assertNotNull(savedTemperaDevice.getId(), "Saved TemperaDevice ID is null");
        Assertions.assertEquals(DeviceStatus.DISABLED, savedTemperaDevice.getStatus(), "Expected TemperaDevice Status to be DISABLED");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDeleteTemperaDevice() throws EntityStillInUseException {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.DISABLED);

        TemperaDevice savedTemperaDevice = temperaDeviceService.save(temperaDevice);
        Assertions.assertNotNull(savedTemperaDevice, "Saved TemperaDevice is null");

        temperaDeviceService.delete(savedTemperaDevice);

        TemperaDevice deletedTemperaDevice = temperaDeviceService.findTemperaDeviceById(savedTemperaDevice.getId());
        Assertions.assertNull(deletedTemperaDevice, "TemperaDevice was not deleted");
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedGetAllTemperaDevices() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> temperaDeviceService.getAllTemperaDevices());
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedCreateTemperaDevice() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> temperaDeviceService.createTemperaDevice());
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedSaveTemperaDevice() {
        TemperaDevice temperaDevice = new TemperaDevice();
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> temperaDeviceService.save(temperaDevice));
    }

    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedDeleteTemperaDevice() {
        TemperaDevice temperaDevice = new TemperaDevice();
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> temperaDeviceService.delete(temperaDevice));
    }

    @Test
    @Transactional
    public void testRegister() {
        TemperaDevice notRegisteredTemperaDevice = new TemperaDevice();
        notRegisteredTemperaDevice.setStatus(DeviceStatus.NOT_REGISTERED);
        temperaDeviceRepository.save(notRegisteredTemperaDevice);
        Assertions.assertEquals(DeviceStatus.NOT_REGISTERED, temperaDeviceRepository.findTemperaDeviceById(notRegisteredTemperaDevice.getId()).getStatus(), "Status should be NOT_REGISTERED after setting it so");

        temperaDeviceService.register(notRegisteredTemperaDevice);
        Assertions.assertEquals(DeviceStatus.DISABLED, temperaDeviceRepository.findTemperaDeviceById(notRegisteredTemperaDevice.getId()).getStatus(), "Status should have changed to DISABLED after calling register");

        TemperaDevice disabledTemperaDevice = new TemperaDevice();
        disabledTemperaDevice.setStatus(DeviceStatus.DISABLED);
        temperaDeviceRepository.save(disabledTemperaDevice);
        Assertions.assertEquals(DeviceStatus.DISABLED, temperaDeviceRepository.findTemperaDeviceById(disabledTemperaDevice.getId()).getStatus(), "Status should be DISABLED after setting it so");

        temperaDeviceService.register(disabledTemperaDevice);
        Assertions.assertEquals(DeviceStatus.DISABLED, temperaDeviceRepository.findTemperaDeviceById(disabledTemperaDevice.getId()).getStatus(), "Status of enabled device should not change after calling register");

        TemperaDevice enabledTemperaDevice = new TemperaDevice();
        enabledTemperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDeviceRepository.save(enabledTemperaDevice);
        Assertions.assertEquals(DeviceStatus.ENABLED, temperaDeviceRepository.findTemperaDeviceById(enabledTemperaDevice.getId()).getStatus(), "Status should be ENABLED after setting it so");

        temperaDeviceService.register(enabledTemperaDevice);
        Assertions.assertEquals(DeviceStatus.ENABLED, temperaDeviceRepository.findTemperaDeviceById(enabledTemperaDevice.getId()).getStatus(), "Status of enabled device should not change after calling register");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testFindUserOfTemperaDevice() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDeviceRepository.save(temperaDevice);

        Userx user = new Userx();
        user.setTemperaDevice(temperaDevice);
        userxRepository.save(user);

        Userx foundUser = temperaDeviceService.findUserOfTemperaDevice(temperaDevice.getId());
        assertEquals(user, foundUser, "Expected to find the user connected to the TemperaDevice");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetTemperaDevicesByRoom() {
        Room room = new Room();
        room.setRoomNumber("test");
        room = roomRepository.save(room);
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setRoom(room);

        TemperaDevice temperaDevice = new TemperaDevice();
        accessPoint.setTemperaDevices(List.of(temperaDevice));

        accessPointRepository.save(accessPoint);
        temperaDeviceRepository.save(temperaDevice);

        List<TemperaDevice> devices = temperaDeviceService.getTemperaDevicesByRoom(room);
        assertEquals(1, devices.size(), "Expected to find one TemperaDevice connected to the Room");
        assertEquals(temperaDevice, devices.get(0), "Expected to find the TemperaDevice connected to the Room");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testLogStatusChange() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice = temperaDeviceRepository.save(temperaDevice);

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setTemperaDevices(List.of(temperaDevice));
        accessPoint = accessPointRepository.save(accessPoint);

        temperaDeviceService.logStatusChange(temperaDevice);

        List<LogTemperaDevice> logs = logTemperaDeviceRepository.findAll();
        assertEquals(2, logs.size(), "Expected 2 log entries to be saved");
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testLogAccessPointChange() {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice = temperaDeviceRepository.save(temperaDevice);

        AccessPoint oldAccessPoint = new AccessPoint();
        oldAccessPoint.setTemperaDevices(List.of(temperaDevice));
        oldAccessPoint = accessPointRepository.save(oldAccessPoint);

        AccessPoint newAccessPoint = new AccessPoint();
        newAccessPoint = accessPointRepository.save(newAccessPoint);
        temperaDevice.setAccessPoint(newAccessPoint);
        temperaDeviceRepository.save(temperaDevice);

        temperaDeviceService.logAccessPointChange(temperaDevice);

        List<LogTemperaDevice> logs = logTemperaDeviceRepository.findAll();
        assertEquals(4, logs.size(), "Expected 4 log entries to be saved");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testFindTemperaDeviceBySensor() {
        Sensor sensor = new Sensor();
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setSensors(List.of(sensor));

        temperaDeviceRepository.save(temperaDevice);

        TemperaDevice foundDevice = temperaDeviceService.findTemperaDeviceBySensor(sensor);
        assertEquals(temperaDevice, foundDevice, "Expected to find the TemperaDevice connected to the Sensor");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetActiveWarning() {
        TemperaDevice temperaDevice = new TemperaDevice();
        Warning warning = new Warning();
        warning.setSensorType(SensorType.AIR_TEMPERATURE);
        temperaDevice.setWarnings(List.of(warning));

        TemperaDevice savedDevice = temperaDeviceRepository.save(temperaDevice);

        Warning foundWarning = temperaDeviceService.getActiveWarning(savedDevice, SensorType.AIR_TEMPERATURE);
        assertEquals(warning, foundWarning, "Expected to find the active warning for the TemperaDevice and SensorType");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testFindTemperaDeviceByIdWithString() throws IdNotFoundException {
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDeviceRepository.save(temperaDevice);

        String temperaDeviceId = "G4T2-TD-" + temperaDevice.getId();
        TemperaDevice foundDevice = temperaDeviceService.findTemperaDeviceById(temperaDeviceId);
        assertEquals(temperaDevice, foundDevice, "Expected to find the TemperaDevice by its ID string");
    }

    @Test
    @Transactional
    public void testFindTemperaDeviceByIdWithInvalidString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> temperaDeviceService.findTemperaDeviceById("INVALID-TD-1"));
    }

    @Test
    @Transactional
    public void testFindTemperaDeviceByIdWithNonExistentId() {
        Assertions.assertThrows(IdNotFoundException.class, () -> temperaDeviceService.findTemperaDeviceById("G4T2-TD-99999"));
    }
}
