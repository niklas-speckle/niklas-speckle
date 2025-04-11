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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
public class TemperaDevice extends Metadata implements Serializable, Persistable<Long>, Device {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tempera_device_gen")
    @SequenceGenerator(name = "tempera_device_gen", sequenceName = "tempera_device_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    @Enumerated(EnumType.STRING)
    @EqualsAndHashCode.Include
    @Column(columnDefinition = "varchar(20) default 'DISABLED'")
    private DeviceStatus status;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sensor> sensors;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Warning> warnings;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinTable(name = "access_point_tempera_devices",
            joinColumns = @JoinColumn(name = "tempera_devices_id"),
            inverseJoinColumns = @JoinColumn(name = "access_point_id"))
    private AccessPoint accessPoint;

    public boolean isEnabled() {
        return status.equals(DeviceStatus.ENABLED);
    }

    public Sensor getTemperatureSensor() {
        return sensors.stream()
                .filter(sensor -> sensor.getSensorType() == SensorType.AIR_TEMPERATURE)
                .findFirst().orElse(null);
    }

    public Sensor getHumiditySensor() {
        return sensors.stream()
                .filter(sensor -> sensor.getSensorType() == SensorType.AIR_HUMIDITY)
                .findFirst().orElse(null);
    }

    public Sensor getAirQualitySensor() {
        return sensors.stream()
                .filter(sensor -> sensor.getSensorType() == SensorType.AIR_QUALITY)
                .findFirst().orElse(null);
    }

    public Sensor getLightSensor() {
        return sensors.stream()
                .filter(sensor -> sensor.getSensorType() == SensorType.LIGHT_INTENSITY)
                .findFirst().orElse(null);
    }


    @Override
    public boolean isNew() {
        return (id == null);
    }

    /**
     * A TemperaDevice can only be enabled if it was registered.
     * For registration the admin first needs to create a TemperaDevice in the WebApp and needs to assign it to an AccessPoint.
     * The AccessPoint and TemperaDevice (hardware components) need to be set up.
     * Once the AccessPoint connects to this TemperaDevice, the AccessPoint sends a registration message to the server for the TemperaDevice.
     * @return boolean if a TemperaDevice is registered
     */
    public boolean isRegistered() {
        return !status.equals(DeviceStatus.NOT_REGISTERED);
    }

}
