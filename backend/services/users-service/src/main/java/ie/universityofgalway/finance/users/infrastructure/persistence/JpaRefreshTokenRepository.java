package ie.universityofgalway.finance.users.infrastructure.persistence;

import ie.universityofgalway.finance.users.domain.RefreshToken;
import ie.universityofgalway.finance.users.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA implementation of the {@link RefreshTokenRepository} port.
 *
 * <p>Stores refresh tokens through a Spring Data repository and translates between the
 * domain {@link RefreshToken} and the database-facing {@link RefreshTokenEntity}, so the
 * rest of the application never sees JPA types.</p>
 */
@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {


    private final RefreshTokenJpaRepository jpa;

    public JpaRefreshTokenRepository(RefreshTokenJpaRepository jpa){
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken token){
        RefreshTokenEntity saved = jpa.save(toEntity(token));
        return toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash){
        Optional<RefreshTokenEntity> found = jpa.findByTokenHash(tokenHash);
        if (found.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(toDomain(found.get()));
    }

    @Override
    @Transactional
    public void deleteByTokenHash(String tokenHash){
        jpa.deleteByTokenHash(tokenHash);
    }


    /**
     * Converts a domain token ({@link RefreshToken}) into a database entity ({@link RefreshTokenEntity}).
     *
     * @param token the domain token
     * @return the entity holding the same values
     */
    private RefreshTokenEntity toEntity(RefreshToken token){
        return new RefreshTokenEntity(
                token.id(), token.userId(), token.tokenHash(), token.expiresAt()
        );
    }

    /**
     * Converts a database entity ({@link RefreshTokenEntity}) into a domain token ({@link RefreshToken}).
     *
     * @param entity the entity loaded from the database
     * @return the domain token holding the same values
     */
    private RefreshToken toDomain(RefreshTokenEntity entity){
        return new RefreshToken(
                entity.getId(), entity.getUserId(), entity.getTokenHash(), entity.getExpiresAt()
        );
    }
}
