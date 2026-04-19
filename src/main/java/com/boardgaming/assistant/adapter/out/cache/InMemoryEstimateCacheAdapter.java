package com.boardgaming.assistant.adapter.out.cache;

import com.boardgaming.assistant.application.port.out.EstimateCachePort;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryEstimateCacheAdapter implements EstimateCachePort {

    private final ConcurrentHashMap<String, SessionTimingEstimate> store = new ConcurrentHashMap<>();

    @Override
    public Optional<SessionTimingEstimate> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void put(String key, SessionTimingEstimate estimate) {
        store.put(key, estimate);
    }
}
