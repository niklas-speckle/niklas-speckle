package at.qe.skeleton.model.notifications;


import at.qe.skeleton.model.notifications.visitorpattern.APINotificationValidationVisitableInterface;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Getter
@Setter
public abstract class APINotificationDeviceBody extends APINotification implements APINotificationValidationVisitableInterface {

    protected APINotificationDeviceBody() {
        super();
    }

    protected APINotificationDeviceBody(APINotification other) {
        super(other);
    }



}
