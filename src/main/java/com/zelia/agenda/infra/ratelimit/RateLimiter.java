package com.zelia.agenda.infra.ratelimit;

import com.zelia.agenda.exceptions.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas em janela fixa, mantido em memória.
 * Vale para uma única instância da aplicação. Com várias instâncias,
 * substituir por um armazenamento compartilhado (ex.: Redis).
 */
@Component
public class RateLimiter {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int maxAttempts, Duration window) {
        Instant now = Instant.now();

        Window current = windows.compute(key, (k, existing) ->
                existing == null || !now.isBefore(existing.end) ? new Window(now.plus(window)) : existing);

        if (current.increment() > maxAttempts) {
            throw new RateLimitExceededException();
        }

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(w -> !now.isBefore(w.end));
        }
    }

    private static final class Window {
        private final Instant end;
        private int count;

        private Window(Instant end) {
            this.end = end;
        }

        private synchronized int increment() {
            return ++count;
        }
    }
}
