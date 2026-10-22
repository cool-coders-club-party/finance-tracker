package ie.universityofgalway.finance.users.domain;

/**
 * Thrown when someone provides an incorrect email and/or password
 * Intentionally worded to so users can't use the login page to
 * find out what emails are registered.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
