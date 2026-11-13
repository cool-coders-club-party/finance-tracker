package ie.universityofgalway.finance.users.application;

/**
 * A signed access token and its lifetime, issued by {@link TokenIssuer}.
 *
 * <p>The client sends the token in the {@code Authorization: Bearer} header of requests
 * to protected endpoints. This record is returned to the controller, which includes both
 * values in the login and refresh responses.
 *
 * @param value            the encoded JWT string
 * @param expiresInSeconds how long the token is valid for, in seconds from the time it
 *                         was issued
 */
public record AccessToken(String value, long expiresInSeconds) {
}
