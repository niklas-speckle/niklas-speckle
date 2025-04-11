package at.qe.skeleton.model.notifications.visitorpattern;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.notifications.APINotificationAPBody;
import at.qe.skeleton.model.notifications.APINotificationTDBody;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface APINotificationValidationVisitorInterface {

    Optional<ResponseEntity<String>> visit(APINotificationTDBody apiNotificationTDBody, AccessPoint sender) throws EntityValidationException, EntityStillInUseException;

    Optional<ResponseEntity<String>> visit(APINotificationAPBody apiNotificationAPBody, AccessPoint sender);
}
