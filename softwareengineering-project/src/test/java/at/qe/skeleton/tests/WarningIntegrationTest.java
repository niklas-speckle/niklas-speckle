package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.climate.WarningService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@SpringBootTest
public class WarningIntegrationTest {
    @Autowired
    TemperaDeviceRepository temperaDeviceRepository;
    @Autowired
    private ClimateMeasurementRepository climateMeasurementRepository;
    @Autowired
    private SensorRepository sensorRepository;
    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private AccessPointRepository accessPointRepository;
    @Autowired
    private WarningService warningService;
    @Autowired
    private UserxRepository userxRepository;
    @Autowired
    private TimeRecordRepository timeRecordRepository;
    @Autowired
    private RoomRepository roomRepository;

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testWarningService() {
        Limits limits = new Limits();
        limits.setLowerLimit(18);
        limits.setUpperLimit(25);
        limits.setMessageLower("Lower");
        limits.setMessageUpper("Upper");
        limits.setSensorType(SensorType.AIR_TEMPERATURE);

        Room room = new Room();
        room.setRoomNumber("TestRoom");
        room.setLimitsList(List.of(limits));
        room.setCreateDate(LocalDateTime.of(2021, 1, 1, 9, 0, 0));

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setRoom(roomRepository.save(room));

        LocalDateTime timestamp = LocalDateTime.of(2021, 1, 1, 9, 0, 0);
        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setStatus(DeviceStatus.ENABLED);
        temperaDevice.setAccessPoint(accessPointRepository.save(accessPoint));

        Sensor sensor = new Sensor();
        sensor.setSensorType(SensorType.AIR_TEMPERATURE);
        sensor.setClimateMeasurements(new ArrayList<>());
        sensor.setSensorType(SensorType.AIR_TEMPERATURE);
        sensorRepository.save(sensor);

        ClimateMeasurement climateMeasurement = new ClimateMeasurement();
        climateMeasurement.setTimeStamp(timestamp);
        climateMeasurement.setSensor(sensor);
        climateMeasurementRepository.save(climateMeasurement);

        sensor.getClimateMeasurements().add(climateMeasurement);
        sensorRepository.save(sensor);

        temperaDevice.setSensors(new ArrayList<>(List.of(sensor)));
        temperaDeviceRepository.save(temperaDevice);

        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setWorkMode(WorkMode.DEEP_WORK);
        timeRecordRepository.save(timeRecord);

        Userx user = new Userx();
        user.setUsername("test");
        user.setTemperaDevice(temperaDevice);
        user.setTimeRecords(new ArrayList<>(List.of(timeRecord)));
        user.setNotifications(new ArrayList<>());
        userxRepository.save(user);

        Assertions.assertNotNull(sensorRepository.findSensorById(sensor.getId()).getClimateMeasurements());
        Assertions.assertNull(temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings());
        Assertions.assertEquals(sensor, temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getSensors().get(0));

        Warning oldWarning = new Warning();
        oldWarning.setTimestamp(LocalDateTime.of(2021, 1, 1, 6, 0, 0));
        oldWarning.setSensorType(SensorType.AIR_TEMPERATURE);
        oldWarning.setWarningStatus(WarningStatus.DRAFT);
        warningRepository.save(oldWarning);
        temperaDevice.setWarnings(new ArrayList<>(List.of(oldWarning)));
        temperaDeviceRepository.save(temperaDevice);

        Assertions.assertEquals(1, temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings().size());
        Warning beforeDeletion = temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings().get(0);
        warningService.checkWarning(climateMeasurement);
        Warning afterDeletion = temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings().get(0);
        Assertions.assertEquals(1, temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings().size());
        Assertions.assertNotEquals(beforeDeletion, afterDeletion);

        climateMeasurement.setTimeStamp(LocalDateTime.of(2021, 1, 1, 9, 6, 0));
        climateMeasurementRepository.save(climateMeasurement);
        warningService.checkWarning(climateMeasurement);
        Assertions.assertEquals(WarningStatus.UNSEEN,
                temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings().get(0).getWarningStatus());


        climateMeasurement.setTimeStamp(LocalDateTime.of(2021, 1, 1, 9, 40, 0));
        climateMeasurementRepository.save(climateMeasurement);

        timeRecord.setUser(userxRepository.save(user));
        timeRecordRepository.save(timeRecord);

        warningService.checkWarning(climateMeasurement);
        List<Warning> newWarnings = temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()).getWarnings();
        Assertions.assertEquals(1,newWarnings.size());
        Assertions.assertEquals(WarningStatus.DRAFT, newWarnings.get(0).getWarningStatus());








    }
}
