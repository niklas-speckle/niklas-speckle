package at.qe.skeleton.model.notifications.visitorpattern;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.notifications.Notification;

public interface NotificationButtonVisitableInterface {

    void accept(NotificationButtonVisitorInterface visitor, Notification notification) throws EntityValidationException;
}
