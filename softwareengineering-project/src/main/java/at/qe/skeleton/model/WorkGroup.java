package at.qe.skeleton.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = "users")
@Entity(name = "workgroup")
@EntityListeners(AuditingEntityListener.class)
public class WorkGroup extends Metadata implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "work_group_gen")
    @SequenceGenerator(name = "work_group_gen", sequenceName = "work_group_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 1024)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "userx_workgroup",
            joinColumns = @JoinColumn(name = "workgroup_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<Userx> users;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "workgroup_project",
            joinColumns = @JoinColumn(name = "workgroup_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id"))
    private Set<Project> projects;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_leader")
    private Userx groupLeader;

    @Column(columnDefinition = "boolean default false")
    private boolean disabled;

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkGroup workGroup = (WorkGroup) o;
        return Objects.equals(id, workGroup.id) && Objects.equals(name, workGroup.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }


    @Override
    public boolean isNew() {
        // AuditAspect needs to determine weather an entity is new (log create action) or is being updated (log update action).
        // Logging methods are called after the entity is saved, so the id is already set at this point.
        // To avoid always getting update logs --> check if updateDate equals createDate (both are set at creation, updateDate is changed on every persisted update).
        return (id == null || getUpdateDate() == getCreateDate());
    }

    /**
     * Returns the users of the work group as a concatenated string. Used for filtering workGroups by users in the ui.
     * @return
     */
    public String getUsersAsString(){
        if(users == null || users.isEmpty()){
            return "";
        }
        return users.stream().map(Userx::getUsername).reduce("", (a, b) -> a + ", " + b);
    }
}
