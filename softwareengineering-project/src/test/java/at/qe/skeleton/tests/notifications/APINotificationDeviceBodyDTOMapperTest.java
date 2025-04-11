package at.qe.skeleton.tests.notifications;

import at.qe.skeleton.model.DeviceType;
import at.qe.skeleton.model.notifications.APINotificationAPBody;
import at.qe.skeleton.model.notifications.APINotificationDeviceBody;
import at.qe.skeleton.model.notifications.APINotificationTDBody;
import at.qe.skeleton.model.notifications.NotificationType;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import at.qe.skeleton.rest.mapper.APINotificationDeviceBodyDTOMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@SpringBootTest
public class APINotificationDeviceBodyDTOMapperTest {

    @Autowired
    private APINotificationDeviceBodyDTOMapper apiNotificationDeviceBodyDTOMapper;

    @Test
    public void testAPINotificationDTOMapper_mapFrom() {

        APINotificationDTO apiNotificationDTOAP = APINotificationDTO.builder()
                .deviceId(1001L)
                .deviceType("AP")
                //Warning
                .notificationType("1")
                .message("message")
                .timestamp(null)
                .build();


        APINotificationDeviceBody apiNotificationAP = apiNotificationDeviceBodyDTOMapper.mapFrom(apiNotificationDTOAP);

        Assertions.assertEquals(apiNotificationDTOAP.deviceId(), apiNotificationAP.getDeviceId());
        Assertions.assertEquals(DeviceType.ACCESS_POINT, apiNotificationAP.getDeviceType());
        Assertions.assertEquals(NotificationType.INFO, apiNotificationAP.getNotificationType());
        Assertions.assertTrue(ChronoUnit.SECONDS.between(apiNotificationAP.getTimestamp(), LocalDateTime.now()) < 0.1);
        Assertions.assertEquals(apiNotificationDTOAP.message(), apiNotificationAP.getMessage());

        APINotificationDTO apiNotificationDTOTD = APINotificationDTO.builder()
                .deviceId(1001L)
                .deviceType("TD")
                //Warning
                .notificationType("2")
                .message("message")
                .timestamp(null)
                .build();


        APINotificationDeviceBody apiNotificationTD = apiNotificationDeviceBodyDTOMapper.mapFrom(apiNotificationDTOTD);

        Assertions.assertEquals(apiNotificationDTOTD.deviceId(), apiNotificationTD.getDeviceId());
        Assertions.assertEquals(DeviceType.TEMPERA_DEVICE, apiNotificationTD.getDeviceType());
        Assertions.assertEquals(NotificationType.WARNING, apiNotificationTD.getNotificationType());
        Assertions.assertTrue(ChronoUnit.SECONDS.between(apiNotificationTD.getTimestamp(), LocalDateTime.now()) < 0.1);
        Assertions.assertEquals(apiNotificationDTOTD.message(), apiNotificationTD.getMessage());

        APINotificationDTO apiNotificationDTOInvalid1 = APINotificationDTO.builder()
                .deviceId(1001L)
                .deviceType("AD")
                //Invalid
                .notificationType("3")
                .message("message")
                .timestamp(null)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            apiNotificationDeviceBodyDTOMapper.mapFrom(apiNotificationDTOInvalid1);
        });

        APINotificationDTO apiNotificationDTOInvalid2 = APINotificationDTO.builder()
                .deviceId(1001L)
                .deviceType("AP")
                //Invalid
                .notificationType("4")
                .message("message")
                .timestamp(null)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            apiNotificationDeviceBodyDTOMapper.mapFrom(apiNotificationDTOInvalid2);
        });
    }


    @Test
    public void testAPINotificationDTOMapper_mapTo() {

        APINotificationDeviceBody apiNotificationAP = new APINotificationAPBody();
        apiNotificationAP.setDeviceId(1001L);
        apiNotificationAP.setDeviceType(DeviceType.ACCESS_POINT);
        apiNotificationAP.setNotificationType(NotificationType.INFO);
        apiNotificationAP.setMessage("message");
        apiNotificationAP.setTimestamp(LocalDateTime.now());

        APINotificationDTO apiNotificationDTOAP = apiNotificationDeviceBodyDTOMapper.mapTo(apiNotificationAP);

        Assertions.assertEquals(apiNotificationDTOAP.deviceId(), apiNotificationAP.getDeviceId());
        Assertions.assertEquals(apiNotificationDTOAP.deviceType(), apiNotificationAP.getDeviceType().getAbbreviatedString());
        Assertions.assertEquals(apiNotificationDTOAP.notificationType(), String.valueOf(apiNotificationAP.getNotificationType().getValue()));
        Assertions.assertEquals(apiNotificationDTOAP.message(), apiNotificationAP.getMessage());
        Assertions.assertEquals(apiNotificationDTOAP.timestamp(), apiNotificationAP.getTimestamp());

        APINotificationDeviceBody apiNotificationTD = new APINotificationTDBody();
        apiNotificationTD.setDeviceId(1001L);
        apiNotificationTD.setDeviceType(DeviceType.TEMPERA_DEVICE);
        apiNotificationTD.setNotificationType(NotificationType.WARNING);
        apiNotificationTD.setMessage("message");
        apiNotificationTD.setTimestamp(LocalDateTime.now());

        APINotificationDTO apiNotificationDTOTD = apiNotificationDeviceBodyDTOMapper.mapTo(apiNotificationTD);

        Assertions.assertEquals(apiNotificationDTOTD.deviceId(), apiNotificationTD.getDeviceId());
        Assertions.assertEquals(apiNotificationDTOTD.deviceType(), apiNotificationTD.getDeviceType().getAbbreviatedString());
        Assertions.assertEquals(apiNotificationDTOTD.notificationType(), String.valueOf(apiNotificationTD.getNotificationType().getValue()));
        Assertions.assertEquals(apiNotificationDTOTD.message(), apiNotificationTD.getMessage());
        Assertions.assertEquals(apiNotificationDTOTD.timestamp(), apiNotificationTD.getTimestamp());


    }

    @Test
    public void testAPINotificationDTOMapper_mapFromNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            apiNotificationDeviceBodyDTOMapper.mapFrom(null);
        });
    }


    @Test
    public void testAPINotificationDTOMapper_mapToNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            apiNotificationDeviceBodyDTOMapper.mapTo(null);
        });
    }
}
