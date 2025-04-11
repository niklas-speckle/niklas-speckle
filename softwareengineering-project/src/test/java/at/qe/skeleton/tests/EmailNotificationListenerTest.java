package at.qe.skeleton.tests;

import at.qe.skeleton.services.notifications.EmailNotificationListener;
import at.qe.skeleton.services.notifications.NotificationEvent;
import at.qe.skeleton.model.*;
import at.qe.skeleton.services.EmailService;
import at.qe.skeleton.services.TemperaDeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class EmailNotificationListenerTest {
    @MockBean
    private EmailService emailService;

    @MockBean
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private EmailNotificationListener emailNotificationListener;

    @Test
    public void testOnApplicationEvent() {
        // Given
        Limits limits = Limits.builder()
                .sensorType(SensorType.AIR_QUALITY)
                .lowerLimit(30)
                .upperLimit(50)
                .messageLower("messageLower")
                .messageUpper("messageUpper")
                .build();

        Room room = Room.builder()
                .limitsList(List.of(limits))
                .build();

        AccessPoint accessPoint = AccessPoint.builder()
                .room(room)
                .build();

        Sensor sensor = new Sensor();
        sensor.setSensorType(SensorType.AIR_QUALITY);

        TemperaDevice temperaDevice = TemperaDevice.builder()
                .accessPoint(accessPoint)
                .sensors(List.of(sensor))
                .build();

        Token token = new Token();
        token.setContent("token");

        Warning warning = Warning.builder()
                .sensorType(SensorType.AIR_QUALITY)
                .measuredValue(60.0)
                .token(token)
                .warningStatus(WarningStatus.UNSEEN)
                .build();

        NotificationEvent event = new NotificationEvent(this, temperaDevice, warning);

        when(temperaDeviceService.findUserOfTemperaDevice(temperaDevice.getId())).thenReturn(new Userx());
        doNothing().when(emailService).sendEmail(any(), any(), any());

        // When
        emailNotificationListener.onApplicationEvent(event);

        // Then
        verify(emailService, times(1)).sendEmail(any(), any(), any());
        verify(temperaDeviceService, times(1)).findUserOfTemperaDevice(temperaDevice.getId());
        verify(emailService, times(1)).sendEmail(any(), any(), contains(warning.getSensorType().getName()));
        verify(emailService, times(1)).sendEmail(any(), any(), contains(limits.getMessageUpper()));
        verify(emailService, times(1)).sendEmail(any(), any(), contains(token.getContent()));
    }

}
