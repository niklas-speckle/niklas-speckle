package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Setter
@Getter
@Component
@Scope("session")
public class BadgeBean implements Serializable {
    private int numberOfNotifications;

    @Autowired
    private SessionInfoBean sessionInfoBean;

    @Autowired
    private transient NotificationService notificationService;


    @PostConstruct
    public void init() {
        try{
            numberOfNotifications = (int) notificationService.getNumberOfNotifications(sessionInfoBean.getCurrentUser());
        } catch (NullPointerException e) {
            numberOfNotifications = 0;
        }
    }

    public void update() {
        init();
    }
}
