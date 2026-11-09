package ie.universityofgalway.finance.users.domain;

import java.util.UUID;
import java.time.Instant;

/**
 * Domain model of what a Refresh Token should be.
 * A refresh token lets a user obtain a new access token
 * without logging in again. It lives longer than an Access Token.
 *
 * <p>
 * Only a SHA-256 hash of the token is stored. So a database leak
 * cannot be used to refresh sessions as the raw value is never stored.
 * The raw token is returned to the client once (when it is issued).
 * Tokens are valid for 7 days by default, configured via
 * {@code app.jwt.refresh-expiry-days}
 *
 * @param id ID of the RefreshToken
 * @param userId ID of the user who owns the RefreshToken
 * @param tokenHash A SHA-256 hash of the raw token (id, userId and expiresAt)
 * @param expiresAt Expiration date of the token
 */
public record RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt) {

    /**
     * Like with {@link User}, the hash is a sensitive. {@code tokenHash} is
     * the lookup key for the token and should be kept out of logs.
     * @return the token's id, userId and expiresAt
     */
    @Override
    public String toString(){
        return "RefreshToken[id="+id+", userId="+userId+", expiresAt="+expiresAt+"]";
    }
}
