package ie.universityofgalway.finance.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID>{

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);
}
