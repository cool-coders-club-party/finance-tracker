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
     * swaps a valid token for a new one
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
     * Logout. Deletes a refresh token
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
     *
     * @param rawToken
     * @return
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
