package ie.universityofgalway.finance.users.infrastructure.security;

import ie.universityofgalway.finance.users.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

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
