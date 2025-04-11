package at.qe.skeleton.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * Data transfer object for API notifications.
 * @param timestamp can be null, in which case the current timestamp is used
 * @param deviceType should be either "AP" or "TD"
 * @param deviceId id of device as number
 * @param notificationType should be either "1" for INFO or "2" for WARNING or "3" for ERROR
 * @param message notification content
 */
@Builder
public record APINotificationDTO(
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("device_type") String deviceType,
        @JsonProperty("device_id") Long deviceId,
        @JsonProperty("message_type") String notificationType,
        @JsonProperty("message") String message
) implements Serializable {
}
