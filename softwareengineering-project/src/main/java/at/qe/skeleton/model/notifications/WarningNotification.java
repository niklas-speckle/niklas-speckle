package at.qe.skeleton.model.notifications;

import at.qe.skeleton.model.Token;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.util.List;
import java.util.function.Supplier;

@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WarningNotification extends Notification {

    /**
     * The default buttons for a WarningNotification.
     * WarningNotifications can be CONFIRMED or IGNORED, which will set the correspondening Warning respectively.
     * Afterwards the notification will be deleted.
     */
    public static final List<Supplier<NotificationButton>> DEFAULT_BUTTONS = List.of(
            NotificationConfirmButton::new,
            NotificationIgnoreButton::new
    );

    @OneToOne
    @JoinColumn(name = "token_content")
    private Token token;

    /**
     * Copy constructor
     * @param other the Notification to copy
     */
    public WarningNotification(WarningNotification other){
        super(other);
        this.token = other.token;
        super.setButtons(DEFAULT_BUTTONS.stream().map(Supplier::get).toList());
    }


    /**
     * NoArgsConstructor with List<NotificationButtonAction> init
     * @return a new WarningNotification
     */
    public WarningNotification(){
        super();
        super.setButtons(DEFAULT_BUTTONS.stream().map(Supplier::get).toList());
    }

    @Override
    public String getHeader() {
        return "Room Climate Violation: ";
    }
}
