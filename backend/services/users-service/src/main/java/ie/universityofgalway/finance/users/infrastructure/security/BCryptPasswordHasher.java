package ie.universityofgalway.finance.users.infrastructure.security;

import ie.universityofgalway.finance.users.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordHasher} implementation that uses BCrypt through Spring Security's
 * {@link BCryptPasswordEncoder}.
 *
 * <p>BCrypt adds a random salt to every hash, so hashing the same password twice gives
 * different results. That is why passwords are checked with {@link #matches(String, String)}
 * and not by hashing again and comparing strings.</p>
 *
 * <p>Note: The encoder uses its default work factor (10). Higher numbers increase the time to hash.
 * This makes brute forcing more costly to do, but you also pay the cost on every login and registration.</p>
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String password){
        return encoder.encode(password);
    }

    @Override
    public boolean matches(String password, String hash){
        return encoder.matches(password, hash);
    }
}
