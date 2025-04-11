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
@EqualsAndHashCode(exclude = {"users", "workGroups"})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Project extends Metadata implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_gen")
    @SequenceGenerator(name = "project_gen", sequenceName = "project_seq", allocationSize = 1, initialValue = 100)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(columnDefinition = "boolean default false")
    private boolean disabled;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_manager")
    private Userx projectManager;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "userx_project",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<Userx> users;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "workgroup_project",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "workgroup_id"))
    private List<WorkGroup> workGroups;


    @Override
    public String toString() {
        return name;
    }


    @Override
    public boolean isNew() {
        return (id == null);
    }


    /**
     * Returns the work group ids as a concatenated string. Used for filtering projects by work groups
     */
    public String getWorkGroupIDsAsString(){
        if(workGroups == null || workGroups.isEmpty()){
            return "";
        }
        return workGroups.stream().map(WorkGroup::getId).map(String::valueOf).reduce((s, s2) -> s + " " + s2).orElse("");
    }

    /**
     * Returns the user names as a concatenated string. Used for filtering projects by users
     */
    public String getUserNamesAsString() {
        if (users == null || users.isEmpty()) {
            return "";
        }
        return users.stream().map(Userx::getUsername).reduce((s, s2) -> s + " " + s2).orElse("");
    }
}
