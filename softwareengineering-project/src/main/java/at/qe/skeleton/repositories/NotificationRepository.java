package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.notifications.Notification;

import java.util.List;

public interface NotificationRepository extends AbstractRepository<Notification, Long> {
    List<Notification> findAllByUser(Userx user);
}
