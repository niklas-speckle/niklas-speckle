package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.notifications.Notification;
import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
@Scope("view")
public class NotificationListController implements Serializable {

    @Autowired
    private SessionInfoBean sessionInfoBean;

    @Autowired
    private transient NotificationService notificationService;

    public List<Notification> getNotificationsOfCurrentUser() {
        return notificationService.getNotificationsByUser(sessionInfoBean.getCurrentUser());
    }
}
