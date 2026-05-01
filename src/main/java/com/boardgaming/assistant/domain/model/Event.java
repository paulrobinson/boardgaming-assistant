package com.boardgaming.assistant.domain.model;

import java.time.Instant;
import java.util.Map;

public record Event(
        EventType type,
        Map<String, String> payload,
        Instant timestamp) {

    public static Event of(EventType type, Map<String, String> payload) {
        return new Event(type, Map.copyOf(payload), Instant.now());
    }
}
