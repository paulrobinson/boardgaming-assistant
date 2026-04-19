package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.SessionTimingEstimate;

import java.util.Optional;

public interface EstimateCachePort {
    Optional<SessionTimingEstimate> get(String key);

    void put(String key, SessionTimingEstimate estimate);
}
