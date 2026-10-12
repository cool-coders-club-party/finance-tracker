package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.EmailAlreadyRegisteredException;
import ie.universityofgalway.finance.users.domain.InvalidCredentialsException;
import ie.universityofgalway.finance.users.domain.User;
import ie.universityofgalway.finance.users.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository users, PasswordHasher passwordHasher){
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Creates a new user account
     * @param email email used in registration. Trailing spaces and case are ignored
     * @param password plaintext password (will be hashed later)
     * @return newly created User object
     * @throws EmailAlreadyRegisteredException if the email has been already taken
     */
    public User register(String email, String password){
        // normalise email given address
        String normalisedEmail = email.trim().toLowerCase();

        // check if the email is already registered to an account
        if (users.existsByEmail(normalisedEmail)){
            throw new EmailAlreadyRegisteredException(normalisedEmail);
        }

        // hash password
        String hash = passwordHasher.hash(password);
        return users.save(new User(UUID.randomUUID(), normalisedEmail, hash));
    }

    /**
     * Checks emails against the user repository.
     * Hashes given passwords and checks against known hashes
     * @param email given email
     * @param password given password
     * @return user object
     */
    public User login(String email, String password){
        String normalisedEmail = email.trim().toLowerCase();


        Optional<User> searchResult = users.findByEmail(normalisedEmail);
        if (searchResult.isEmpty()){
            throw new InvalidCredentialsException();
        }

        User user = searchResult.get();

        if(!passwordHasher.matches(password, user.passwordHash())){
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
