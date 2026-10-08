package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.EmailAlreadyRegisteredException;
import ie.universityofgalway.finance.users.domain.User;
import ie.universityofgalway.finance.users.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users){
        this.users = users;
    }

    /**
     * Creates a new user account
     * @param email email used in registration. Trailing spaces and case are ignored
     * @param password plaintext password (will be hashed later)
     * @return newly created user object
     * @throws EmailAlreadyRegisteredException if the email has been already taken
     */
    public User register(String email, String password){
        String normalisedEmail = email.trim().toLowerCase();

        if (users.existsByEmail(normalisedEmail)){
            throw new EmailAlreadyRegisteredException(normalisedEmail);
        }

        return users.save(new User(UUID.randomUUID(), normalisedEmail));
    }
}
