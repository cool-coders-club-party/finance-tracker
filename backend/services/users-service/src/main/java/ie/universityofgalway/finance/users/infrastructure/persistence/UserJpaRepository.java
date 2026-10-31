package ie.universityofgalway.finance.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * JpaRepository (through CrudRepository) provides save, findById and more for the entity type UserEntity with a UUID key
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID>{
    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);
}
