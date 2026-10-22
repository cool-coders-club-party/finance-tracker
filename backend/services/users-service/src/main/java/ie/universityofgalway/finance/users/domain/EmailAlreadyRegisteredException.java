package ie.universityofgalway.finance.users.domain;

/**
 * Thrown when someone tries to register with an email that already has an
 * account associated with it
 */
public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("An account already exists for "+email+".");
    }
}
