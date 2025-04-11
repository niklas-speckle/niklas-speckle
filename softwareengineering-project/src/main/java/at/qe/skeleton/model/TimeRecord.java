package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.MINUTES;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class TimeRecord extends Metadata implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_record_gen")
    @SequenceGenerator(name = "time_record_gen", sequenceName = "time_record_seq", allocationSize = 1, initialValue = 100)
    @Column(name = "id")
    private Long id;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Userx user;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "workgroup_id")
    private WorkGroup workGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false)
    private WorkMode workMode;


    public Long getDuration() {
        if (this.endTime != null) {
            return MINUTES.between(this.startTime, this.endTime);
        } else {
            return MINUTES.between(this.startTime, LocalDateTime.now());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRecord that = (TimeRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public boolean isNew() {
      return (id == null);
    }

}
