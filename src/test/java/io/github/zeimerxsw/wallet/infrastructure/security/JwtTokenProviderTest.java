package io.github.zeimerxsw.wallet.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider =
            new JwtTokenProvider("test-secret-for-unit-tests-32-chars!!", 3600000L);

    @Test
    void generateToken_producesValidToken() {
        String token = provider.generateToken("user@example.com");
        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider("test-secret-for-unit-tests-32-chars!!", 1L);
        String token = shortLived.generateToken("user@example.com");
        Thread.sleep(10);
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void isValid_withGarbageString_returnsFalse() {
        assertThat(provider.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        String token = provider.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThat(provider.isValid(tampered)).isFalse();
    }
}
