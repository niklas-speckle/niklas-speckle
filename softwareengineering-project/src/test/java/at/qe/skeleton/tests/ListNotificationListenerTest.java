package at.qe.skeleton.tests;

import at.qe.skeleton.services.notifications.ListNotificationListener;
import at.qe.skeleton.services.notifications.NotificationEvent;
import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.Notification;
import at.qe.skeleton.model.notifications.WarningNotification;
import at.qe.skeleton.repositories.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class ListNotificationListenerTest {
    @Autowired
    private UserxRepository userxRepository;
    @Autowired
    private LimitsRepository limitsRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AccessPointRepository accessPointRepository;
    @Autowired
    private TemperaDeviceRepository temperaDeviceRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private ListNotificationListener listNotificationListener;
    @Autowired
    private NotificationRepository notificationRepository;

    @Transactional
    @Test
    public void integrationTestListNotificationListener() {
        // add Notification
        // Given
        Limits limits = Limits.builder()
                .sensorType(SensorType.AIR_QUALITY)
                .lowerLimit(30)
                .upperLimit(50)
                .messageLower("messageLower")
                .messageUpper("messageUpper")
                .build();

        limitsRepository.save(limits);

        Room room = new Room();
        room.setRoomNumber("testRoom");
        room.setLimitsList(new ArrayList<>(List.of(limits)));
        room.setCreateDate(LocalDateTime.now());

        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setRoom(roomRepository.save(room));

        accessPointRepository.save(accessPoint);

        Sensor sensor = new Sensor();
        sensor.setSensorType(SensorType.AIR_QUALITY);
        sensor.setSensorUnit(SensorUnit.PPM);
        sensor.setClimateMeasurements(new ArrayList<>());

        TemperaDevice temperaDevice = new TemperaDevice();
        temperaDevice.setSensors(new ArrayList<>(List.of(sensor)));
        temperaDevice.setAccessPoint(accessPoint);

        temperaDeviceRepository.save(temperaDevice);

        Warning warning = Warning.builder()
                .sensorType(SensorType.AIR_QUALITY)
                .measuredValue(60.0)
                .warningStatus(WarningStatus.UNSEEN)
                .timestamp(LocalDateTime.now())
                .build();

        warningRepository.save(warning);

        Token token = new Token("testToken", warning);

        warning.setToken(token);
        warningRepository.save(warning);

        temperaDevice.setWarnings(new ArrayList<>(List.of(warning)));
        temperaDeviceRepository.save(temperaDevice);

        Userx user = new Userx();
        user.setUsername("testUser");
        user.setTemperaDevice(temperaDevice);
        user.setNotifications(new ArrayList<>());
        userxRepository.save(user);

        NotificationEvent event = new NotificationEvent(this, temperaDevice, warning);

        // When
        listNotificationListener.onApplicationEvent(event);

        // Then
        List<Notification> notifications = notificationRepository.findAllByUser(user);
        Assertions.assertEquals(1, notifications.size(), "Notification was not added.");
        Assertions.assertTrue(notifications.get(0) instanceof WarningNotification, "Notification is not a WarningNotification.");
        Assertions.assertEquals("Room Climate Violation: ", notifications.get(0).getHeader(), "Header is not correct.");
        Assertions.assertEquals("Air Quality is too high.", notifications.get(0).getMessage(), "Message is not correct.");

        // delete the notification
        // Given
        warning.setWarningStatus(WarningStatus.CONFIRMED);

        // When
        listNotificationListener.onApplicationEvent(event);

        // Then
        notifications = notificationRepository.findAllByUser(user);
        Assertions.assertEquals(0, notifications.size(), "Notification was not deleted.");

        // another notification
        // Given
        Warning newWarning = Warning.builder()
                .sensorType(SensorType.AIR_QUALITY)
                .measuredValue(60.0)
                .warningStatus(WarningStatus.UNSEEN)
                .timestamp(LocalDateTime.now())
                .build();

        warningRepository.save(newWarning);

        Token newToken = new Token("newTestToken", newWarning);

        newWarning.setToken(newToken);
        warningRepository.save(newWarning);

        NotificationEvent newEvent = new NotificationEvent(this, temperaDevice, newWarning);

        // When
        listNotificationListener.onApplicationEvent(newEvent);

        // Then
        notifications = notificationRepository.findAllByUser(user);
        Assertions.assertEquals(1, notifications.size());
        Assertions.assertTrue(notifications.get(0) instanceof WarningNotification);

    }
}

