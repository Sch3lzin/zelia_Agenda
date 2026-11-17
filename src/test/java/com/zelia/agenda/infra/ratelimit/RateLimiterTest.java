package com.zelia.agenda.infra.ratelimit;

import com.zelia.agenda.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter();
        Duration janela = Duration.ofMinutes(1);

        for (int i = 0; i < 3; i++) {
            limiter.check("chave", 3, janela);
        }

        assertThatThrownBy(() -> limiter.check("chave", 3, janela))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void differentKeysAreIndependent() {
        RateLimiter limiter = new RateLimiter();
        Duration janela = Duration.ofMinutes(1);

        limiter.check("ip:1", 1, janela);

        assertThatCode(() -> limiter.check("ip:2", 1, janela)).doesNotThrowAnyException();
    }

    @Test
    void counterResetsAfterWindow() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        Duration janela = Duration.ofMillis(50);

        limiter.check("chave", 1, janela);
        assertThatThrownBy(() -> limiter.check("chave", 1, janela))
                .isInstanceOf(RateLimitExceededException.class);

        Thread.sleep(80);

        assertThatCode(() -> limiter.check("chave", 1, janela)).doesNotThrowAnyException();
    }
}
