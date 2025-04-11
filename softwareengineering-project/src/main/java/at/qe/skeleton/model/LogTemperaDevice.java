package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class LogTemperaDevice implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "log_tempera_device_gen")
    @SequenceGenerator(name = "log_tempera_device_gen", sequenceName = "log_tempera_device_seq", allocationSize = 1, initialValue = 100)
    private Long id;
    private LocalDateTime timestamp;
    private Long temperaDeviceId;

    @Enumerated(EnumType.STRING)
    private LogStatus logStatus;

    @Enumerated(EnumType.STRING)
    private DeviceStatus newStatus;

}
