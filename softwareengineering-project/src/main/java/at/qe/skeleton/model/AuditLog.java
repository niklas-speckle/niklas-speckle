package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
public class AuditLog implements Persistable<Long>, Serializable, Comparable<AuditLog> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String authenticatedUser;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private ActionStatus status;

    private String accessedResource;

    private String additionalDetails;

    @Override
    public int compareTo(AuditLog o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog logEntry = (AuditLog) o;
        return Objects.equals(getId(), logEntry.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "{" +
                "action=" + action +
                ", status=" + status +
                ", accessedResource='" + accessedResource + '\'' +
                ", authenticatedUser='" + authenticatedUser + '\'' +
                ", additionalDetails='" + additionalDetails + '\'' +
                '}';
    }
}
