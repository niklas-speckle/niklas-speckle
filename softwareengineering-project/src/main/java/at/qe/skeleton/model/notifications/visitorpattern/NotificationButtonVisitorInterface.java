package at.qe.skeleton.model.notifications.visitorpattern;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.notifications.Notification;
import at.qe.skeleton.model.notifications.NotificationConfirmButton;
import at.qe.skeleton.model.notifications.NotificationDeleteButton;
import at.qe.skeleton.model.notifications.NotificationIgnoreButton;

public interface NotificationButtonVisitorInterface {

    void visit(NotificationDeleteButton button, Notification notification) throws EntityValidationException;
    void visit(NotificationConfirmButton button, Notification notification);
    void visit(NotificationIgnoreButton button, Notification notification);
}
