package ie.universityofgalway.finance.users.domain;

import java.util.Optional;

/**
 * Storage operations the application needs for users.
 */
public interface UserRepository {

    /* Stores the user and returns it */
    User save(User user);

    /* Returns true if a user with this email already exists */
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
