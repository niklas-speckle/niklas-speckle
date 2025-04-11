package at.qe.skeleton.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


public record MeasurementDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("temperaDeviceId") Long temperaDeviceId,
        @JsonProperty("air_temperature") float airTemperature,
        @JsonProperty("air_humidity") float airHumidity,
        @JsonProperty("air_quality") float airQuality,
        @JsonProperty("light_intensity") float lightIntensity
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

}

