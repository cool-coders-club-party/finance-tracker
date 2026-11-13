package ie.universityofgalway.finance.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link UserEntity} rows, keyed by a {@link UUID}.
 *
 * <p>Spring generates the implementation at startup. Through {@link JpaRepository} it
 * already provides {@code save}, {@code findById} and other standard operations; the methods
 * below are derived queries, where Spring builds the database query from the method name.
 * This interface is internal to the persistence package: the rest of the application uses the
 * domain {@code UserRepository}, implemented by {@link JpaUserRepository}.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID>{

    /**
     * Checks whether a user with the given email exists. Derived query on the
     * {@code email} field.
     *
     * @param email the email to look for
     * @return {@code true} if a row with that email exists, otherwise {@code false}
     */
    boolean existsByEmail(String email);

    /**
     * Finds the user with the given email. Derived query on the {@code email} field.
     *
     * @param email the email to look for
     * @return the matching entity, or an empty {@link Optional} if none exists
     */
    Optional<UserEntity> findByEmail(String email);
}
