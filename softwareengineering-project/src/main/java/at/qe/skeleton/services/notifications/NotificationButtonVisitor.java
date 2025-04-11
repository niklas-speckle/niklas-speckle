package at.qe.skeleton.services.notifications;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.model.notifications.visitorpattern.NotificationButtonVisitorInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class NotificationButtonVisitor implements NotificationButtonVisitorInterface {

    @Autowired
    private NotificationService notificationService;

    public void execute(Notification notification) throws EntityValidationException {
        for (NotificationButton button : notification.getButtons()) {
            button.accept(this, notification);
        }
    }

    public void visit(NotificationDeleteButton button, Notification notification) throws EntityValidationException {
        notificationService.deleteNotification(notification);
    }

    public void visit(NotificationConfirmButton button, Notification notification) {
        notificationService.confirmNotification(notification);
    }

    public void visit(NotificationIgnoreButton button, Notification notification) {
        notificationService.ignoreNotification(notification);
    }
}
