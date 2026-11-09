package ie.universityofgalway.finance.users.domain;

import ie.universityofgalway.finance.users.api.GlobalExceptionHandler;

/**
 * Thrown when a token hash can't be used (unknown or expired)
 * Mapped to 401 UNAUTHORISED in {@link GlobalExceptionHandler}
 * <p>
 * Intentionally kept vague so a caller can't tell if the
 * token ever existed.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
