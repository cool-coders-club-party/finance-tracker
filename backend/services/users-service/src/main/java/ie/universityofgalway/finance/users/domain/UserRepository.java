package ie.universityofgalway.finance.users.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and retrieving {@link User}s
 *
 * <p>Defines the storage operations the application needs for users.
 * It is implemented in the infrastructure layer, so the application
 * code doesn't depend on a particular database.</p>
 */
public interface UserRepository {

    /**
     * Store a user in the database
     *
     * @param user the user to store
     * @return the stored user
     */
    User save(User user);

    /* Returns true if a user with this email already exists */

    /**
     * Checks whether a user with the given email already exists
     *
     * @param email the email to check
     * @return {@code true} if a user with the given email exists, otherwise {@code false}
     */
    boolean existsByEmail(String email);

    /**
     * Searches the database for a user by email
     *
     * @param email the email to search for
     * @return the matching user, or an empty {@link Optional} if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Searches the database for a user by ID
     *
     * @param id the ID of the user
     * @return the matching user, or an empty {@link Optional} if none exists
     */
    Optional<User> findById(UUID id);
}
