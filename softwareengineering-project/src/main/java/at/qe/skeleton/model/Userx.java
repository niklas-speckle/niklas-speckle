package at.qe.skeleton.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import at.qe.skeleton.model.notifications.Notification;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity representing users.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Userx extends Metadata implements Persistable<Long>, Serializable, Comparable<Userx> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_gen")
    @SequenceGenerator(name = "user_gen", sequenceName = "user_seq", allocationSize = 1, initialValue = 100)
    private Long id;


    @Column(unique = true, length = 100)
    private String username;

    private String password;
    private String firstName;
    private String lastName;

    @ManyToMany
    @JoinTable(
            name = "userx_workgroup",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "workgroup_id"))
    private Set<WorkGroup> workGroups;

    @ManyToMany
    @JoinTable(
            name = "userx_project",
            joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "project_id") } )
    private Set<Project> projects;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<TimeRecord> timeRecords;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<Notification> notifications = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    private TemperaDevice temperaDevice;

    @ManyToOne
    @JoinColumn(name = "default_project")
    private Project defaultProject;

    @Enumerated(EnumType.STRING)
    private WorkModeVisibility workModeVisibility;

    private String email;
    private String phone;

    boolean enabled = true;

    @ElementCollection(targetClass = UserxRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "Userx_UserxRole")
    @Enumerated(EnumType.STRING)
    private List<UserxRole> roles;


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.username);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Userx)) {
            return false;
        }
        final Userx other = (Userx) obj;
        return Objects.equals(this.username, other.username);
    }

    @Override
    public String toString() {
        return "at.qe.skeleton.model.User[ id=" + username + " ]";
    }

    @Override
    public boolean isNew() {
        return (id == null);
    }

	@Override
	public int compareTo(Userx o) {
		return this.username.compareTo(o.getUsername());
	}

    public boolean hasRole(UserxRole role){
        if(roles == null || roles.isEmpty()){
            return false;
        }
        return roles.contains(role);
    }

}