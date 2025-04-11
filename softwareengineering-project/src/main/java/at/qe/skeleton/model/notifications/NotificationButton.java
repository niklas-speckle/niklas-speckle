package at.qe.skeleton.model.notifications;


import at.qe.skeleton.model.notifications.visitorpattern.NotificationButtonVisitableInterface;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

@Entity
public abstract class NotificationButton implements Persistable<Long>, Serializable, NotificationButtonVisitableInterface {

    @Id
    @Getter
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "not_button_gen")
    @SequenceGenerator(name = "not_button_gen", sequenceName = "not_button_gen", allocationSize = 1, initialValue = 100)
    private Long id;


    /**
     * The value of the button to be shown in the UI.
     */
    String label;

    public boolean isNew(){
        return id == null;
    }


    public abstract String getLabel();

}
