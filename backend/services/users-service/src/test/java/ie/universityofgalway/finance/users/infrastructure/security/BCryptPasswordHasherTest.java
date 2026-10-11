package ie.universityofgalway.finance.users.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashIsNotThePlainPassword() {
        String hash = hasher.hash("12345678");

        assertThat(hash).isNotEqualTo("12345678");
        assertThat(hash).startsWith("$2");
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = hasher.hash("12345678");

        assertThat(hasher.matches("12345678", hash)).isTrue();
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hash = hasher.hash("12345678");

        assertThat(hasher.matches("87654321", hash)).isFalse();
    }

    @Test
    void hashingSamePasswordTwiceGivesDifferentHashes() {
        String first = hasher.hash("12345678");
        String second = hasher.hash("12345678");

        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches("12345678", first)).isTrue();
        assertThat(hasher.matches("12345678", second)).isTrue();
    }
}
