package at.qe.skeleton.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record LogTemperaDeviceDTO(
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("logStatus") String logStatus,
        @JsonProperty("temperaDeviceId") Long temperaDeviceId,
        @JsonProperty("newStatus") String newStatus

)  implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

}
