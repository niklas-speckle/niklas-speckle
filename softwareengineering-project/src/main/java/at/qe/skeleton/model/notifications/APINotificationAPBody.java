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
public class APINotificationAPBody extends APINotificationDeviceBody {

    public APINotificationAPBody() {
        super();
    }

    public APINotificationAPBody(APINotification other) {
        super(other);
        if(super.getDeviceType().equals(DeviceType.TEMPERA_DEVICE)){
            throw new IllegalArgumentException("Device type not supported.");
        }
    }

    @Override
    public Optional<ResponseEntity<String>> accept(APINotificationValidationVisitorInterface validatorVisitor, AccessPoint accessPoint) throws EntityValidationException, EntityStillInUseException {
        return validatorVisitor.visit(this, accessPoint);
    }
}
