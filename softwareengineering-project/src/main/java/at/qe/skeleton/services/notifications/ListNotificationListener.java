package at.qe.skeleton.services.notifications;

import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.WarningNotification;
import at.qe.skeleton.services.TemperaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ListNotificationListener implements ApplicationListener<NotificationEvent> {
    @Autowired
    private TemperaDeviceService temperaDeviceService;
    @Autowired
    private NotificationService notificationService;

    /**
     * This method listens for changes in the WarningStatus of a Warning and reacts as follows:
     * if the new WarningStatus is UNSEEN a respective WarningNotification is added to the User's notification bell;
     * if the new WarningStatus is CONFIRMED, IGNORED or DELETED, the WarningNotification is deleted out of the User's
     * notification list.
     * @param event the NotificationEvent that is published by the WarningService, when a WarningStatus changes.
     */
    @Override
    public void onApplicationEvent(NotificationEvent event) {
        if(event.getWarning().getWarningStatus().equals(WarningStatus.UNSEEN)) {
            Userx user = temperaDeviceService.findUserOfTemperaDevice(event.getTemperaDevice().getId());
            String header = "Room Climate Violation";
            String message = getMessage(event);

            WarningNotification warningNotification = new WarningNotification();
            warningNotification.setUser(user);
            warningNotification.setHeader(header);
            warningNotification.setMessage(message);
            warningNotification.setToken(event.getWarning().getToken());

            notificationService.addNotificationToBell(warningNotification);
            return;
        }
        if(event.getWarning().getWarningStatus().equals(WarningStatus.CONFIRMED) || event.getWarning().getWarningStatus().equals(WarningStatus.IGNORED) || event.getWarning().getWarningStatus().equals(WarningStatus.DELETED)){
            Userx user = temperaDeviceService.findUserOfTemperaDevice(event.getTemperaDevice().getId());

            notificationService.deleteNotification(user, event.getWarning().getToken());
        }
    }

private String getMessage(NotificationEvent event) {
    TemperaDevice temperaDevice = event.getTemperaDevice();
    Warning warning = event.getWarning();

    List<Limits> limits = temperaDevice.getAccessPoint().getRoom().getLimitsList();
    SensorType sensorType = warning.getSensorType();

    Optional<Limits> limit = limits.stream()
            .filter(l -> l.getSensorType().equals(sensorType))
            .findFirst();

    return limit
            .map(l -> warning.getMeasuredValue() < l.getLowerLimit()
                    ? sensorType.getName() + " is too low."
                    : sensorType.getName() + " is too high.")
            .orElse("Room Climate Violation detected.");
}

}
