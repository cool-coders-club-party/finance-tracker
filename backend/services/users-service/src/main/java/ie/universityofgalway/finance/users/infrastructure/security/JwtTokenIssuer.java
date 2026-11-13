package ie.universityofgalway.finance.users.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import ie.universityofgalway.finance.users.application.AccessToken;
import ie.universityofgalway.finance.users.application.TokenIssuer;
import ie.universityofgalway.finance.users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * {@link TokenIssuer} implementation that creates signed JWT access tokens.
 *
 * <p>Tokens are signed with HMAC-SHA256 (HS256) using the shared secret from the
 * {@code app.jwt.secret} property. The same secret is used by the decoder in
 * {@link SecurityConfig#jwtDecoder(String)} to verify them, so the two must remain consistent</p>
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final Duration lifetime;
    
    // "${app.jwt.secret}" tells spring to grab the parameter from application.yml
    /**
     * Creates the issuer from the application configuration. Spring supplies the arguments
     * from the {@code app.jwt.secret} and {@code app.jwt.expiry-minutes} properties in {@code application.yml}.
     *
     * @param secret        the signing secret; must be at least 32 bytes (256 bits), as
     *                      HMAC-SHA256 requires
     * @param expiryMinutes how many minutes an issued access token stays valid
     * @throws IllegalStateException if the secret is shorter than 32 bytes
     */
    public JwtTokenIssuer(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiry-minutes}") long expiryMinutes){
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if(length < 32) { // I had a & instead of a $ 2 lines above, so the secret was smaller than required. This exception would have saved the hour of debugging.
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes but was " + length + " (is the JWT_SECRET environment variable set?)");
        }

        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.lifetime = Duration.ofMinutes(expiryMinutes);
    }

    @Override
    public AccessToken issue(User user){
        Instant now = Instant.now();
        
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString()) // sub - registered claim
                .claim("email", user.email()) // email - public claim
                .issuedAt(now) // iat - registered claim
                .expiresAt(now.plus(lifetime)) // exp - registered claim
                .build();
        
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, lifetime.toSeconds());
    }
}
