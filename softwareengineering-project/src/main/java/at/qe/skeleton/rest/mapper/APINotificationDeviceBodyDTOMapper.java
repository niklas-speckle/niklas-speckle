package at.qe.skeleton.rest.mapper;

import at.qe.skeleton.model.DeviceType;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class APINotificationDeviceBodyDTOMapper implements DTOMapper<APINotificationDeviceBody, APINotificationDTO>{
    @Override
    public APINotificationDeviceBody mapFrom(APINotificationDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO must not be null.");
        }

        APINotificationDeviceBody apiNotification;
        if(dto.deviceType().equals("AP")){
            apiNotification = new APINotificationAPBody();
        } else if(dto.deviceType().equals("TD")){
            apiNotification = new APINotificationTDBody();
        } else {
            throw new IllegalArgumentException("Device type not supported.");
        }

        apiNotification.setTimestamp(dto.timestamp() == null ? LocalDateTime.now() : dto.timestamp());
        apiNotification.setDeviceType(DeviceType.fromValue(dto.deviceType()));
        apiNotification.setDeviceId(dto.deviceId());
        apiNotification.setNotificationType(NotificationType.fromValue(Integer.parseInt(dto.notificationType())));
        apiNotification.setMessage(dto.message());

        return apiNotification;
    }

    @Override
    public APINotificationDTO mapTo(APINotificationDeviceBody entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null.");
        }

        return APINotificationDTO.builder()
                .notificationType(String.valueOf(entity.getNotificationType().getValue()))
                .deviceId(entity.getDeviceId())
                .deviceType(entity.getDeviceType().getAbbreviatedString())
                .message(entity.getMessage())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
