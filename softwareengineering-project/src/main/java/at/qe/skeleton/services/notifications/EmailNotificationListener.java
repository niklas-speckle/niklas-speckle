package at.qe.skeleton.services.notifications;

import at.qe.skeleton.model.*;
import at.qe.skeleton.services.EmailService;
import at.qe.skeleton.services.TemperaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EmailNotificationListener implements ApplicationListener<NotificationEvent> {
    @Autowired
    private EmailService emailService;

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    /**
     * This method listens for changes in the WarningStatus of a Warning and sends an email to the respective user if
     * the new WarningStatus is UNSEEN.
     * @param event the NotificationEvent that is published by the WarningService, when a WarningStatus changes.
     */
    @Override
    public void onApplicationEvent(NotificationEvent event) {
        if(event.getWarning().getWarningStatus().equals(WarningStatus.UNSEEN)) {
            Userx user = temperaDeviceService.findUserOfTemperaDevice(event.getTemperaDevice().getId());
            String msg = getMessage(event);

            emailService.sendEmail(user, "New Room Climate Violation", msg);
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

        String suggestion = limit
                .map(l -> warning.getMeasuredValue() < l.getLowerLimit()
                        ? l.getMessageLower()
                        : l.getMessageUpper())
                .orElse("No suggestion available.");

        String link = "http://localhost:8080/api/warnings"
                + "?token=" + event.getWarning().getToken().getContent()  + "&status=";

        return "A violation of your room climate limits regarding " + sensorType.getName() + " was detected.<br>"
                + "Suggestions: " + suggestion + "<br>"
                + "To confirm this warning please click <a href='" + link + WarningStatus.CONFIRMED.ordinal() + "'>CONFIRM</a>.<br><br>"
                + "To ignore this warning please click <a href='" + link + WarningStatus.IGNORED.ordinal() + "'>IGNORE</a>.";
    }

}
