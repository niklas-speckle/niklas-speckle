package at.qe.skeleton.model.notifications;

import at.qe.skeleton.model.DeviceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@SuperBuilder
@Getter
@Setter
@Entity
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class APINotification extends Notification {

    /**
     * The default buttons for an APINotification.
     * APINotifications can only be confirmed which leads to a deletion of the notification.
     */
    private static final List<Supplier<NotificationButton>> DEFAULT_BUTTONS = List.of(
            NotificationDeleteButton::new
    );



    private LocalDateTime timestamp;
    private DeviceType deviceType;
    private Long deviceId;
    private NotificationType notificationType;

    /**
     * Copy constructor
     * @param other the Notification to copy
     */
    public APINotification(APINotification other){
        super(other);
        this.timestamp = other.timestamp;
        this.deviceType = other.deviceType;
        this.notificationType = other.notificationType;
        super.setButtons(DEFAULT_BUTTONS.stream().map(Supplier::get).toList());
    }


    /**
     * NoArgsConstructor with List<NotificationButtonAction> init
     * @return
     */
    public APINotification(){
        super();
        super.setButtons(DEFAULT_BUTTONS.stream().map(Supplier::get).toList());
    }

    @Override
    public String getHeader() {
        return deviceType.getAbbreviatedString();
    }

}
