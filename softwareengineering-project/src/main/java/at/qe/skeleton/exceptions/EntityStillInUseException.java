package at.qe.skeleton.exceptions;



/**
 * Exception thrown when a room is still connected with tempera devices.
 */
public class EntityStillInUseException extends Exception {

    public EntityStillInUseException(String message) {
        super(message);
    }
}
