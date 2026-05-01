package com.boardgaming.assistant.adapter.out.cache;

import com.boardgaming.assistant.application.port.out.EstimateCachePort;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryEstimateCacheAdapter implements EstimateCachePort {

    private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryEstimateCacheAdapter() {
        this(Clock.systemUTC());
    }

    public InMemoryEstimateCacheAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<SessionTimingEstimate> get(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (clock.instant().isAfter(entry.expiresAt())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.estimate());
    }

    @Override
    public void put(String key, SessionTimingEstimate estimate, Duration ttl) {
        Instant expiresAt = clock.instant().plus(ttl);
        store.put(key, new CacheEntry(estimate, expiresAt));
    }

    record CacheEntry(SessionTimingEstimate estimate, Instant expiresAt) {
    }
}
