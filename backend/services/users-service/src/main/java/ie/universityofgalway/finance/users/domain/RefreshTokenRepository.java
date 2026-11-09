package ie.universityofgalway.finance.users.domain;

import java.util.Optional;

/**
 * Port for storing and retrieving {@link RefreshToken}s
 * <p>
 * Implemented in the infrastructure layer.
 * Tokens are identified by their SHA-256 hash, not their raw values.
 */
public interface RefreshTokenRepository {

    /**
     * Store a refresh token
     * @param token the raw token to store
     * @return the stored token, unhashed
     */
    RefreshToken save(RefreshToken token);

    // Search and return a token if it exists

    /**
     * Search for a token by its hash
     * @param tokenHash SHA-265 hash of token (caller will have to hash the raw token first)
     * @return matching token, or empty if non exists
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Delete a token if it exists

    /**
     * Deletes a token with the given hash.
     * Used on logout or when a token is rotated.
     * Does nothing if no such token exists
     * @param tokenHash SHA-256 hash of the token to be deleted
     */
    void deleteByTokenHash(String tokenHash);
}
