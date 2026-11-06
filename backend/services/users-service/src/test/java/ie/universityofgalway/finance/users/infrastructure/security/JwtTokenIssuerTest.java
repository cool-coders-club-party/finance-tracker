package ie.universityofgalway.finance.users.infrastructure.security;

import ie.universityofgalway.finance.users.application.AccessToken;
import ie.universityofgalway.finance.users.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that {@link JwtTokenIssuer} produces tokens that other services can
 * trust: correctly signed, carrying the agreed claims, expiring on time, and
 * never containing anything sensitive.
 * <p>
 * Notes for future me:
 * SecretKeySpec is an implementation of SecretKey that creates a secret key from a byte array.
 * The second argument is a string used to specify the algorithm to use.
 * <p>
 * HS256 (HMAC SHA-256) should be at least 256 bits/32 bytes. JJWT will throw {@code WeakKeyException}
 * if a short key is used to sign in or verify.
 */
public class JwtTokenIssuerTest {
    // HS256 needs a secret of at least 32 bytes
    private static final String SECRET = "test-secret-32-bytes-minimum-123"; // 32 bytes
    private static final String OTHER_SECRET = "a-different-test-secret-also-32-bytes-minimum"; // 45 bytes

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(SECRET, 15);
    private final User user = new User(UUID.randomUUID(), "test@example.com", "secret-hash");

    /**
     * Helper function that verifies a token like a service would.
     * Check the signature with the secret and the expiry,
     * then return the decoded claims.
     * <p>
     * NimbusJwtDecoder.decode() takes a JWT string (header.payload.signature) and a key.
     * It parses it, checks the signature against the key, and validates the exp and nbf claims if present.
     * It then returns the Jwt, for you to make further assertions on (i.e. check for other claims)
     * @param token A JWT string (header.payload.structure format)
     * @param secret
     * @return
     */
    private Jwt decode(String token, String secret){
        // secret.getBytes(StandardCharsets.UTF_8) converts the secret String to a byte array as HMAC only works on bytes
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build().decode(token);
    }

    @Test
    void tokenCanBeVerifiedWithSecret(){
        AccessToken token = issuer.issue(user);

        Jwt jwt = decode(token.value(), SECRET);
        assertThat(jwt).isNotNull();
    }

    @Test
    void tokenCannotBeVerifiedWithADifferentSecret(){
        AccessToken token = issuer.issue(user);

        assertThrows(JwtException.class, ()-> decode(token.value(), OTHER_SECRET));
    }

    @Test
    void containsTheEmailClaim(){
        Jwt jwt = decode(issuer.issue(user).value(), SECRET);
        assertThat(jwt.getClaimAsString("email")).isEqualTo("test@example.com");
    }

    @Test
    void expiresFifteenMinutesAfterItWasIssued() {
        AccessToken token = issuer.issue(user);
        Jwt jwt = decode(token.value(), SECRET);

        Duration lifetime = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt());

        assertThat(lifetime).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getIssuedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(token.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void doesNotContainThePasswordHash() {
        Jwt jwt = decode(issuer.issue(user).value(), SECRET);

        assertThat(jwt.getClaims()).doesNotContainKey("passwordHash");
        assertThat(jwt.getClaims().toString()).doesNotContain("super-secret-hash");
    }
}
