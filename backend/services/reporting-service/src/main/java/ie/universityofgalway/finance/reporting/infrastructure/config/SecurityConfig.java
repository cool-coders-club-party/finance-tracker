package ie.universityofgalway.finance.reporting.infrastructure.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the Reporting service as a stateless OAuth2 resource server
 * that authenticates requests using bearer JWTs.
 */
@Configuration
public class SecurityConfig {

    /**
     * Defines the HTTP authentication rules for the service.
     *
     * <p>Authentication uses bearer tokens instead of HTTP sessions, so CSRF
     * protection is disabled. The chain permits unauthenticated {@code OPTIONS}
     * requests and access to {@code /actuator/health} and {@code /actuator/info}.
     * All other requests require authentication.</p>
     *
     * @param http Spring Security builder for the filter chain
     * @return the configured stateless filter chain
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()).oauth2ResourceServer(oauth ->
                        oauth.jwt(Customizer.withDefaults())).build();
    }

    /**
     * Creates the decoder that verifies HS256-signed access tokens.
     *
     * @param secret shared signing secret from the {@code jwt.secret} property
     * @return the decoder configured to verify tokens with the shared secret
     * @throws IllegalStateException if the secret is shorter than 32 bytes in UTF-8
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }

        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
