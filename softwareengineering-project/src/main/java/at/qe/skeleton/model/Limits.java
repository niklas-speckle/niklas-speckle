package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Limits extends Metadata implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "limit_gen")
    @SequenceGenerator(name = "limit_gen", sequenceName = "limit_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    private double upperLimit;
    private double lowerLimit;
    private String messageLower;
    private String messageUpper;
    private String reasonForChange;



    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private SensorType sensorType;

    @ManyToOne
    @JoinColumn(name = "room_number")
    private Room room;

    public boolean isValid(){
        return upperLimit >= lowerLimit;
    }


    /**
     * Copy constructor for creating Limits from DefaultLimits
     * @param limits
     */
    public Limits(Limits limits) {
        this.upperLimit = limits.getUpperLimit();
        this.lowerLimit = limits.getLowerLimit();
        this.messageLower = limits.getMessageLower();
        this.messageUpper = limits.getMessageUpper();
        this.sensorType = limits.getSensorType();
    }

    @Override
    public boolean isNew() {
        return (id == null);
    }
}
