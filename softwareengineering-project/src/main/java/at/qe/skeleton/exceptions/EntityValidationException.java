package at.qe.skeleton.exceptions;



/**
 * Exception thrown when an entity is not valid (e.g. constraints are not met).
 */
public class EntityValidationException extends Exception {

    public EntityValidationException(String message) {
        super(message);
    }
}
