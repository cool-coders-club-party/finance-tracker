package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service that handles registration, login and user lookup.
 *
 * <p>This class holds the business rules for accounts:
 * <ul>
 *     <li>Emails are normalised before they are stored or searched</li>
 *     <li>Passwords are only ever stored as hashes</li>
 *     <li>Failed logins give no hint about whether the email or the password was wrong</li>
 * </ul>
 *
 * It has no knowledge of HTTP, the controller layer calls it and turns its results and exceptions into responses.
 */
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
     *
     * <p>The email is normalised (whitespace removed and converted to lower case)
     * before it is checked against the database and stored.
     *
     * The password is hashed before the user is saved, so the plaintext never stored</p>
     *
     * @param email email used in registration. Trailing spaces and case are ignored
     * @param password plaintext password to be hashed
     * @return newly created {@link User} object
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
     * Checks emails and passwords against the database.
     * <p>Email is normalised in the same way as {@link #register(String, String)}
     * The given password is checked against the stored hash for the given user</p>
     *
     * <p>An unknown email or an incorrect password throw the same exception, so a caller
     * can't use the endpoint to find out which emails have accounts</p>
     * @param email given email, trailing spaces and case are ignored
     * @param password plaintext password
     * @return authenticated {@link User} object
     * @throws InvalidCredentialsException if no email or password is found
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

    /**
     * Helper method to find a user by ID for a refresh token.
     * A refresh token that has no user is invalid, so is an
     * unknown token. They throw the same exception to prevent
     * someone from using the endpoints to brute force valid tokens
     *
     * @param id the user to search the database for
     * @return the user (if found) otherwise, otherwise an empty Optional.
     * @throws InvalidRefreshTokenException if no user has the given id
     */
    public User getUser(UUID id){
        Optional<User> found = users.findById(id);
        if(found.isEmpty()){
            throw new InvalidRefreshTokenException();
        }
        return found.get();
    }
}
