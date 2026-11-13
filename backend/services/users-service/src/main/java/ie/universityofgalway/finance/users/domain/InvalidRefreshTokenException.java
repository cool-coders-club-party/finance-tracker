package ie.universityofgalway.finance.users.domain;

/**
 * Thrown when a refresh token can't be used (unknown or expired)
 *
 * <p>Mapped to {@code 401 Unauthorized} in {@link ie.universityofgalway.finance.users.api.GlobalExceptionHandler}</p>
 *
 * <p>Intentionally kept vague so a caller can't tell if the token ever existed.</p>
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
