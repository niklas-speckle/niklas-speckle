package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = "climateMeasurements")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Sensor extends Metadata implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sensor_gen")
    @SequenceGenerator(name = "sensor_gen", sequenceName = "sensor_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private SensorType sensorType;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private SensorUnit sensorUnit;

    @OneToMany(mappedBy = "id")
    private List<ClimateMeasurement> climateMeasurements;


    @Override
    public boolean isNew() {
       return (id == null);
    }

}
