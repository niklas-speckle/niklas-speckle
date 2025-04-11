package at.qe.skeleton.exceptions;


/**
 * When saving a new entity is attempted with an id that already exists in the DB.
 */
public class IdAlreadyExistsException extends Exception {

        public IdAlreadyExistsException(String message) {
            super(message);
        }
}
