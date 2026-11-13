package ie.universityofgalway.finance.users.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping of the {@code refresh_tokens} table. Persistence only.
 *
 * <p>The rest of the application works with the domain {@link ie.universityofgalway.finance.users.domain.RefreshToken} record.
 * {@link JpaRefreshTokenRepository} converts between the two.
 * Each row is one refresh token identified by the hash of the raw token, the user it belongs to and when it expires.
 */
@Entity
@Table(name="refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id; // token ID

    @Column(name="user_id", nullable=false)
    private UUID userId;

    @Column(name="token_hash", nullable=false, unique=true)
    private String tokenHash;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    protected RefreshTokenEntity(){
        // needed for reflection because I've defined a constructor below
    }

    public RefreshTokenEntity(UUID id, UUID userId, String tokenHash, Instant expiresAt){
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId(){ return id; }
    public UUID getUserId(){ return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt(){ return expiresAt; }
}
