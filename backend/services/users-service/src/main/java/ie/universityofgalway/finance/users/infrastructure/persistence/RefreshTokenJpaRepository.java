package ie.universityofgalway.finance.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link RefreshTokenEntity} rows.
 *
 * <p>Spring generates the implementation at startup. The methods below are derived queries,
 * where Spring builds the database query from the method name. This interface is internal to
 * the persistence package: the rest of the application uses the domain
 * {@code RefreshTokenRepository}, implemented by {@link JpaRefreshTokenRepository}.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID>{

    /**
     * Finds the token whose hash matches. Derived query on the {@code tokenHash} field.
     *
     * @param tokenHash the SHA-256 hash of the raw token
     * @return the matching entity, or an empty {@link Optional} if none exists
     */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Deletes the token whose hash matches. Derived query on the {@code tokenHash} field.
     *
     * <p>Does nothing if no token matches. Derived delete queries need an active
     * transaction, which {@link JpaRefreshTokenRepository#deleteByTokenHash(String)}
     * provides.
     *
     * @param tokenHash the SHA-256 hash of the raw token
     */
    void deleteByTokenHash(String tokenHash);
}
