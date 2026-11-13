package ie.universityofgalway.finance.users.domain;

import java.util.Optional;

/**
 * Port for storing and retrieving {@link RefreshToken}s
 *
 * <p>Implemented in the infrastructure layer.
 * Tokens are identified by their SHA-256 hash, not their raw values.</p>
 */
public interface RefreshTokenRepository {

    /**
     * Store a refresh token
     *
     * @param token the token to store, (contains the ID, userID, hash of the raw token and expiration date)
     * @return the stored token
     */
    RefreshToken save(RefreshToken token);

    /**
     * Search for a token by its hash
     *
     * @param tokenHash SHA-265 hash of token (caller will have to hash the raw token first)
     * @return matching token, or empty if none exists
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Deletes a token with the given hash.
     *
     * <p>Used on logout or when a token is rotated.
     * Does nothing if no such token exists</p>
     *
     * @param tokenHash SHA-256 hash of the token to be deleted
     */
    void deleteByTokenHash(String tokenHash);
}
