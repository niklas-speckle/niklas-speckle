package at.qe.skeleton.services.notifications;

import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.Token;
import at.qe.skeleton.model.Warning;
import org.springframework.context.ApplicationEvent;

public class NotificationEvent extends ApplicationEvent {

    private TemperaDevice temperaDevice;
    private Warning warning;
    private Token token;

    public NotificationEvent(Object source, TemperaDevice temperaDevice, Warning warning) {
        super(source);
        this.temperaDevice = temperaDevice;
        this.warning = warning;
    }

    public TemperaDevice getTemperaDevice() {
        return temperaDevice;
    }

    public void setTemperaDevice(TemperaDevice temperaDevice) {
        this.temperaDevice = temperaDevice;
    }

    public Warning getWarning() {
        return warning;
    }

    public void setWarning(Warning warning) {
        this.warning = warning;
    }

    public Token getToken() {
        return token;
    }
}
