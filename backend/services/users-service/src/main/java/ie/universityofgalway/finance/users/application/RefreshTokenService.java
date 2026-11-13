package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.InvalidRefreshTokenException;
import ie.universityofgalway.finance.users.domain.RefreshToken;
import ie.universityofgalway.finance.users.domain.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; // random number generator, safe for security
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the lifecycle of refresh tokens.
 *
 * <p>A refresh token is a random value that a client exchanges for a new
 * access token once theirs expires. Only a SHA-256 hash of each token is stored in the database.
 * This ensures the database does not reveal valid tokens in the event of a leak.
 * The raw token is returned to the client once, when it's created by {@link #issue(UUID)}</p>
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final Duration lifetime;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-expiry-days}") long expiryDays){
        this.repository = repository;
        this.lifetime = Duration.ofDays(expiryDays);
    }

    /**
     * Creates and stores a new refresh token for the user.
     * The user stores the raw token returned by the method
     * and uses it in future calls to get new access tokens
     * and rotate longer-lived refresh tokens
     * @param userId the user the token will be issued to
     * @return the raw token
     */
    public String issue(UUID userId){
        String rawToken = generateToken();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(), userId, hash(rawToken), Instant.now().plus(lifetime));
        repository.save(token);
        return rawToken;
    }

    /**
     * Swaps a valid refresh token for a new one (created by {@link #issue(UUID)})
     *
     * Clients pass their raw tokens to this method,
     * The raw token is hashed and then checked against the database.
     * If a match is found, the token is deleted from the database
     * {@link #issue(UUID)} is called to issue a new refresh token.
     *
     * @param rawToken token to be rotated
     * @return the rotated token
     */
    @Transactional
    public RotatedToken rotate(String rawToken){
        String tokenHash = hash(rawToken);
        Optional<RefreshToken> found = repository.findByTokenHash(tokenHash);
        if (found.isEmpty()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = found.get();

        repository.deleteByTokenHash(tokenHash); // a token can only be used once
        if (stored.expiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        String newRawToken = issue(stored.userId());
        return new RotatedToken(stored.userId(), newRawToken);
    }

    /**
     * The mechanism that drives "logout" functionality.
     * Deletes a refresh token from the database.
     * This will block the user from interacting with the
     * service until they log in again.
     * @param rawToken token to be deleted
     */
    @Transactional
    public void revoke(String rawToken){
        repository.deleteByTokenHash(hash(rawToken));
    }

    /**
     * Generates a new 256-bit token.<p>
     * Creates a byte array 32 bytes long to store the token<p>
     * {@link SecureRandom#nextBytes(byte[])} generates a random 32 byte value
     * and populates the byte array.<p>
     * Encodes the byte array to String in Base64URL
     *
     * @return Base64URL encoded token as a String
     */
    private String generateToken() {
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes); // SecureRandom.getBytes
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a raw token with SHA-256 for safe storage
     *
     * <p>The same input produces the same hash, so a token can be hashed again and
     * checked against the stored value. No need to store the raw token server-side</p>
     *
     * @param rawToken the raw token to hash
     * @return the SHA-256 hash as a lowercase hexadecimal string
     * @throws IllegalStateException if the SHA-256 algorithm is not available in the JVM
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
