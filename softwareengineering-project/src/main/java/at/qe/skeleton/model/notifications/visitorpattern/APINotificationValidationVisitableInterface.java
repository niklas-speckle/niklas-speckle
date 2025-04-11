package at.qe.skeleton.model.notifications.visitorpattern;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.AccessPoint;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface APINotificationValidationVisitableInterface {

    Optional<ResponseEntity<String>> accept(APINotificationValidationVisitorInterface visitor, AccessPoint sender) throws EntityValidationException, EntityStillInUseException;
}
