package ie.universityofgalway.finance.users.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import ie.universityofgalway.finance.users.application.AccessToken;
import ie.universityofgalway.finance.users.application.TokenIssuer;
import ie.universityofgalway.finance.users.domain.User;
//import io.jsonwebtoken.security.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final Duration lifetime;
    
    // "${app.jwt.secret}" tells spring to grab the parameter from application.yml
    public JwtTokenIssuer(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.expiry-minutes}") long expiryMinutes){
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
