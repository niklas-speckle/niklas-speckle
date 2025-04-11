package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Entity
public class ClimateMeasurement implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "climate_measurement_gen")
    @SequenceGenerator(name = "climate_measurement_gen", sequenceName = "climate_measurement_seq", allocationSize = 1, initialValue = 1000)
    @Column(name = "id")
    private Long id;

    private LocalDateTime timeStamp;

    @ManyToOne
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    private double measuredValue;
}
