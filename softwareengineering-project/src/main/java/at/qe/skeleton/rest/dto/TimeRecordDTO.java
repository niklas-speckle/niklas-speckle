package at.qe.skeleton.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


public record TimeRecordDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("temperaDeviceId") Long temperaDeviceId,
        @JsonProperty("workMode") String workMode
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

}

