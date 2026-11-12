package ie.universityofgalway.finance.users.application;

import ie.universityofgalway.finance.users.domain.InvalidRefreshTokenException;
import ie.universityofgalway.finance.users.domain.RefreshToken;
import ie.universityofgalway.finance.users.domain.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenServiceTest {

    private final FakeRefreshTokenRepository repository = new FakeRefreshTokenRepository();
    private final RefreshTokenService service = new RefreshTokenService(repository, 7);
    private final UUID userId = UUID.randomUUID();

    @Test
    void issueReturnsAToken() {
        String token = service.issue(userId);

        assertThat(token).isNotBlank();
    }

    @Test
    void issueStoresOnlyAHashOfTheToken() {
        String token = service.issue(userId);

        assertThat(repository.size()).isEqualTo(1);
        assertThat(repository.hashes()).doesNotContain(token);
    }

    @Test
    void issueStoresTheUserIdAndAnExpirySevenDaysAway() {
        service.issue(userId);

        RefreshToken stored = repository.tokens().iterator().next();

        assertThat(stored.userId()).isEqualTo(userId);
        assertThat(stored.expiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }

    @Test
    void issuedTokensAreDifferent() {
        String first = service.issue(userId);
        String second = service.issue(userId);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rotateReturnsANewTokenForTheSameUser() {
        String old = service.issue(userId);

        RotatedToken result = service.rotate(old);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.refreshToken()).isNotEqualTo(old);
    }

    @Test
    void rotateReplacesTheOldTokenInsteadOfAddingAnother() {
        String old = service.issue(userId);

        service.rotate(old);

        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void rotateInvalidatesTheOldToken() {
        String old = service.issue(userId);
        service.rotate(old);

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(old));
    }

    @Test
    void theNewTokenCanBeRotatedAgain() {
        String old = service.issue(userId);
        RotatedToken first = service.rotate(old);

        RotatedToken second = service.rotate(first.refreshToken());

        assertThat(second.userId()).isEqualTo(userId);
    }

    @Test
    void rotateRejectsAnUnknownToken() {
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("not-a-real-token"));
    }

    @Test
    void rotateRejectsAnExpiredToken() {
        // a negative lifetime gives a token that has already expired
        RefreshTokenService expiredIssuer = new RefreshTokenService(repository, -1);
        String token = expiredIssuer.issue(userId);

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(token));
    }

    @Test
    void revokeInvalidatesTheToken() {
        String token = service.issue(userId);

        service.revoke(token);

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(token));
    }

    @Test
    void revokingAnUnknownTokenDoesNothing() {
        service.revoke("not-a-real-token");

        assertThat(repository.size()).isZero();
    }

    /** Minimal in-memory repository, keyed by the token hash. */
    private static class FakeRefreshTokenRepository implements RefreshTokenRepository {

        private final Map<String, RefreshToken> byHash = new HashMap<>();

        @Override
        public RefreshToken save(RefreshToken token) {
            byHash.put(token.tokenHash(), token);
            return token;
        }

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void deleteByTokenHash(String tokenHash) {
            byHash.remove(tokenHash);
        }

        int size() {
            return byHash.size();
        }

        Set<String> hashes() {
            return byHash.keySet();
        }

        Collection<RefreshToken> tokens() {
            return byHash.values();
        }
    }
}