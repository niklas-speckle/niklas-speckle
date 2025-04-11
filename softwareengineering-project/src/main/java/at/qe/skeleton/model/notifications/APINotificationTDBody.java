package at.qe.skeleton.model.notifications;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.DeviceType;
import at.qe.skeleton.model.notifications.visitorpattern.APINotificationValidationVisitorInterface;
import jakarta.persistence.Entity;
import lombok.experimental.SuperBuilder;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@SuperBuilder
@Entity
public class APINotificationTDBody extends APINotificationDeviceBody {
    public APINotificationTDBody() {
        super();
    }

    public APINotificationTDBody(APINotification other) {
        super(other);
        if(other.getDeviceType().equals(DeviceType.ACCESS_POINT)){
            throw new IllegalArgumentException("Device type not supported.");
        }
    }

    @Override
    public Optional<ResponseEntity<String>> accept(APINotificationValidationVisitorInterface validatorVisitor, AccessPoint sender) throws EntityValidationException, EntityStillInUseException {
        return validatorVisitor.visit(this, sender);
    }
}
