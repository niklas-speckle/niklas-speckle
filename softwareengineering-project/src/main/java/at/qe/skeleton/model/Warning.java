package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.CascadeType;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"token"})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Warning extends Metadata implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "warning_gen")
    @SequenceGenerator(name = "warning_gen", sequenceName = "warning_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    private LocalDateTime timestamp;

    private Double measuredValue;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(name = "warning_token",
            joinColumns =
                    { @JoinColumn(name = "warning_id", referencedColumnName = "id") },
            inverseJoinColumns =
                    { @JoinColumn(name = "token_content", referencedColumnName = "content") })
    private Token token;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20) default 'DRAFT'")
    private WarningStatus warningStatus;

    private SensorType sensorType;

    @Override
    public boolean isNew() {
        return (id == null);
    }
}
