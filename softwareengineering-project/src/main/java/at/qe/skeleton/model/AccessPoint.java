package at.qe.skeleton.model;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"temperaDevices", "logTemperaDevices"})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class AccessPoint extends Metadata implements Serializable, Persistable<Long>, Device {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_point_gen")
    @SequenceGenerator(name = "access_point_gen", sequenceName = "access_point_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "accessPoint", cascade = CascadeType.PERSIST)
    private List<TemperaDevice> temperaDevices;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20) default 'DISABLED'")
    @Setter(AccessLevel.NONE)
    private DeviceStatus status;

    @ManyToOne
    @Setter
    @Getter
    @JoinColumn(name = "room_number")
    private Room room;

    @OneToMany
    private List<LogTemperaDevice> logTemperaDevices;

    @Setter
    @Getter
    private boolean connected; // is connected to Server

    @Setter
    @Getter
    private LocalDateTime lastConnection; // last connection to Server

    public void setStatus(DeviceStatus status) throws EntityStillInUseException {
        if(status == DeviceStatus.DISABLED) {
            if(this.temperaDevices == null){
                this.status = status;
                return;
            }
            for (TemperaDevice temperaDevice : this.temperaDevices) {
                if (temperaDevice.getStatus() != DeviceStatus.DISABLED) {
                    throw new EntityStillInUseException("AccessPoint still has enabled TemperaDevices.");
                }
            }
        }
        this.status = status;
    }


    @Override
    public boolean isNew() {
        return (id == null);
    }


    /**
     * @return a concatenated string of all TemperaDevice IDs. Used in the frontend to allow for filtering AccessPoints by TemperaDevice ids.
     */
    public String getTemperaDeviceIDsAsString(){
        if(temperaDevices == null || temperaDevices.isEmpty()){
            return "";
        }
        return temperaDevices.stream().map(TemperaDevice::getId).map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("");
    }

    /**
     * An AccessPoint can only be enabled if it was registered.
     * Registration happens on first contact between AccessPoint and Server after the admin created the AccessPoint in the WebApp.
     * Raspberry Pi sends signals for registration after startup to Server.
     * @return boolean if a AccessPoint is registered
     */
    public boolean isRegistered() {
        return !status.equals(DeviceStatus.NOT_REGISTERED);
    }

}
