package ie.universityofgalway.finance.users.infrastructure.persistence;

import ie.universityofgalway.finance.users.domain.User;
import ie.universityofgalway.finance.users.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapter that implements the {@link UserRepository} port using PostgreSQL
 * through Spring Data JPA.
 * <br>
 * Domain {@link User} objects are converted to {@link UserEntity} objects on
 * the way in and back again on the way out, so the rest of the application
 * never handles JPA objects and does not depend on how users are stored.
 * <br><br>
 * Note for future me:<br>
 * In this class you'll see Spring Data's ability to generate database queries
 * derived from method names. These are called derived queries.<br>
 * You can write queries manually with {@code @Query} and bind them to methods
 * Or you can use named queries, defined in the orm.xml
 */
@Repository
public class JpaUserRepository implements UserRepository {
    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Converts a domain {@link User} into the {@link UserEntity} that JPA stores.
     * Prepares a User to be saved into the database, while keeping a clear
     * boundary between the domain and the infrastructure.
     *
     * @param user the domain user to store
     * @return a new entity (instance) holding the same id, email and password hash
     */
    private UserEntity toEntity(User user){
        return new UserEntity(user.id(), user.email(), user.passwordHash());
    }

    /**
     * Converts a {@link UserEntity} loaded from the database into a domain {@link User}.
     * Ensures entities never leave this class, keeping a clear boundary between the
     * domain and the infrastructure.
     *
     * @param entity the entity read from, or just saved to, the users table
     * @return a new domain user holding the same id, email and password hash
     */
    private User toDomain(UserEntity entity){
        return new User(entity.getId(), entity.getEmail(), entity.getPasswordHash());
    }

    /**
     * Stores a user in the users table and returns the stored version.
     * Converts the domain {@link User} to a {@link UserEntity} for JPA, then
     * converts the saved entity back, so callers receive exactly what was stored.
     *
     * @param user the domain user to store
     * @return the stored user as is in the table
     */
    @Override
    public User save(User user){
        UserEntity saved = jpa.save(toEntity(user)); // CrudRepository.save()
        return toDomain(saved);
    }

    /**
     * Checks whether a user with the given email is already stored.
     * Delegates to {@link UserJpaRepository#existsByEmail(String)}.
     * An example of a derived query is {@code jpa.existsByEmail(email)}.
     *
     * @param email the email to look for
     * @return {@code true} if a row with that email exists in the users table
     */
    @Override
    public boolean existsByEmail(String email){
        return jpa.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email){
        /* derived query: jpa.findByEmail(email) */
        Optional<UserEntity> found = jpa.findByEmail(email); // Spring Data builds a database query based on the method name "findByEmail"
        if (found.isEmpty()){ // if no matching email
            return Optional.empty(); // the caller decides how to handle a missing user
        }
        return Optional.of(toDomain(found.get()));
    }
}
