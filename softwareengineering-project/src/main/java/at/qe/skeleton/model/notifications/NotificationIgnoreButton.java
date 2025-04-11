package at.qe.skeleton.model.notifications;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.notifications.visitorpattern.NotificationButtonVisitorInterface;
import jakarta.persistence.Entity;

@Entity
public class NotificationIgnoreButton extends NotificationButton {

    @Override
    public String getLabel() {
        return "Ignore";
    }

    @Override
    public void accept(NotificationButtonVisitorInterface visitor, Notification notification) throws EntityValidationException {
        visitor.visit(this, notification);
    }

}
