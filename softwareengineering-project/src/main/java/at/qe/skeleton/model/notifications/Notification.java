package at.qe.skeleton.model.notifications;

import at.qe.skeleton.model.Userx;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.List;


@SuperBuilder
@Getter
@Setter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class Notification implements Persistable<Long>, Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_gen")
    @SequenceGenerator(name = "notification_gen", sequenceName = "notification_gen", allocationSize = 1, initialValue = 100)
    Long id;


    /**
     * A list of actions a user can take on a type of notification.
     * Necessary for the UI to display the buttons as different notifications have different actions.
     */
    @Getter
    @Setter
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<NotificationButton> buttons;


    @ManyToOne(fetch = FetchType.EAGER)
    @EqualsAndHashCode.Include
    Userx user;

    @Getter
    @Setter
    String message;

    /**
     * Titel of the notification in the UI.
     */
    @Getter
    @Setter
    String header;


    public boolean isNew(){
        return id == null;
    }


    protected Notification(Notification other){
        this.user = other.user;
        this.message = other.message;
        this.header = other.header;
    }

}
