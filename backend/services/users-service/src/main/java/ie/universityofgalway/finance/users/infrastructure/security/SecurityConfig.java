package ie.universityofgalway.finance.users.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security configuration for the Users service
 *
 * <p>The service is a stateless OAuth2 resource server.
 * It keeps no HTTP session and authenticates each request from a JWT access token sent
 * in the {@code Authorization: Bearer} header.
 * Tokens are verified with a shared HMAC secret configured through the {@code app.jwt.secret} property.</p>
 *
 * <p>This class defines the rules for which endpoints are public ({@link #securityFilterChain(HttpSecurity)})
 * and how tokens are verified ({@link #jwtDecoder(String)})</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * Defines the security rules applied to every HTTP request.
     *
     * <p>The rules are:
     * <ul>
     *   <li>CSRF protection is disabled. CSRF guards against attacks that rely on browser
     *       session cookies, and this service uses neither cookies nor sessions.</li>
     *   <li>Sessions are stateless: no {@code HttpSession} is created, so every request
     *       must carry its own access token.</li>
     *   <li>The following endpoints are public: {@code /api/auth/register},
     *       {@code /api/auth/login}, {@code /api/auth/ping}, {@code /api/auth/refresh}
     *       and {@code /api/auth/logout}. The refresh and logout endpoints identify the
     *       caller by the refresh token in the request body, not by an access token.</li>
     *   <li>Every other endpoint requires a valid access token, so any endpoint added
     *       later is protected by default.</li>
     *   <li>Access tokens are validated as JWTs using the {@link JwtDecoder} bean
     *       defined in {@link #jwtDecoder(String)}. Requests with a missing or invalid
     *       token are rejected with {@code 401 Unauthorized}.</li>
     * </ul>
     *
     * @param http the Spring Security builder used to define the filter chain
     * @return the configured filter chain
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/ping", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Creates the decoder that verifies and parses incoming JWT access tokens.
     *
     * <p>Tokens are signed with HMAC-SHA256 using a shared secret, so the secret configured
     * here must be the same one used to sign the tokens. The decoder checks the signature
     * and expiry of each token and exposes its claims as a
     * {@link org.springframework.security.oauth2.jwt.Jwt}.
     *
     * <p>The secret must be at least 32 bytes (256 bits) long (required by HMAC-SHA256).
     * The check runs at startup, so a missing or truncated secret stops the application
     * from starting instead of causing confusing token errors later.
     *
     * @param secret the signing secret, read from the {@code app.jwt.secret} property
     *               (for example from the {@code JWT_SECRET} environment variable)
     * @return a decoder that validates tokens signed with the given secret
     * @throws IllegalStateException if the secret is shorter than 32 bytes
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret){
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if(length < 32){ // I had a & instead of a $ 2 lines above, so the secret was smaller than required. This exception would have saved the hour of debugging.
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes but was " + length + " (is the JWT_SECRET environment variable set?)");
        }

        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
