package com.boardgaming.assistant.adapter.out.cache;

import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.PlayerCountFit;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEstimateCacheAdapterTest {

    private static final Instant BASE_TIME = Instant.parse("2026-05-01T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    private MutableClock clock;
    private InMemoryEstimateCacheAdapter cache;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(BASE_TIME);
        cache = new InMemoryEstimateCacheAdapter(clock);
    }

    @Test
    void returnsEmptyForCacheMiss() {
        assertTrue(cache.get("nonexistent").isEmpty());
    }

    @Test
    void returnsCachedEstimateOnHit() {
        var estimate = createEstimate("est_001");
        cache.put("key1", estimate, TTL);

        var result = cache.get("key1");

        assertTrue(result.isPresent());
        assertEquals("est_001", result.get().estimateId());
    }

    @Test
    void cacheHitReturnsSameEstimate() {
        var estimate = createEstimate("est_001");
        cache.put("key1", estimate, TTL);

        var first = cache.get("key1");
        var second = cache.get("key1");

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get().estimateId(), second.get().estimateId());
    }

    @Test
    void returnsEmptyAfterTtlExpires() {
        var estimate = createEstimate("est_001");
        cache.put("key1", estimate, TTL);

        clock.advance(Duration.ofMinutes(31));

        assertTrue(cache.get("key1").isEmpty());
    }

    @Test
    void returnsEstimateJustBeforeTtlExpires() {
        var estimate = createEstimate("est_001");
        cache.put("key1", estimate, TTL);

        clock.advance(Duration.ofMinutes(29));

        assertTrue(cache.get("key1").isPresent());
    }

    @Test
    void differentKeysAreIndependent() {
        cache.put("key1", createEstimate("est_001"), TTL);
        cache.put("key2", createEstimate("est_002"), TTL);

        assertTrue(cache.get("key1").isPresent());
        assertTrue(cache.get("key2").isPresent());
        assertEquals("est_001", cache.get("key1").get().estimateId());
        assertEquals("est_002", cache.get("key2").get().estimateId());
    }

    @Test
    void overwritesExistingEntry() {
        cache.put("key1", createEstimate("est_001"), TTL);
        cache.put("key1", createEstimate("est_002"), TTL);

        var result = cache.get("key1");

        assertTrue(result.isPresent());
        assertEquals("est_002", result.get().estimateId());
    }

    @Test
    void overwriteResetsExpiration() {
        cache.put("key1", createEstimate("est_001"), TTL);

        clock.advance(Duration.ofMinutes(20));
        cache.put("key1", createEstimate("est_002"), TTL);

        clock.advance(Duration.ofMinutes(20));
        // 40 min since first put, but only 20 since second put — should still be valid
        assertTrue(cache.get("key1").isPresent());
        assertEquals("est_002", cache.get("key1").get().estimateId());
    }

    @Test
    void differentTtlsAreRespected() {
        cache.put("short", createEstimate("est_short"), Duration.ofMinutes(5));
        cache.put("long", createEstimate("est_long"), Duration.ofMinutes(60));

        clock.advance(Duration.ofMinutes(10));

        assertTrue(cache.get("short").isEmpty());
        assertTrue(cache.get("long").isPresent());
    }

    @Test
    void expiredEntryIsRemovedOnGet() {
        cache.put("key1", createEstimate("est_001"), TTL);

        clock.advance(Duration.ofMinutes(31));
        cache.get("key1"); // triggers removal

        // put a new entry with same key — should not see the old one
        cache.put("key1", createEstimate("est_002"), TTL);
        assertEquals("est_002", cache.get("key1").get().estimateId());
    }

    // --- helpers ---

    private SessionTimingEstimate createEstimate(String estimateId) {
        return new SessionTimingEstimate(estimateId, "catan",
                20, 70, 90, Confidence.MEDIUM,
                List.of(new PlayerCountFit(3, Fit.GOOD), new PlayerCountFit(4, Fit.BEST)),
                "Catan plays best at 4.", List.of(),
                BASE_TIME);
    }

    /**
     * A mutable clock for deterministic TTL testing.
     */
    static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
