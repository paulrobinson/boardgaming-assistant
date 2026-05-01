package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.EstimateRequestRecord;

import java.util.Optional;

public interface EstimateRequestPersistencePort {
    void save(EstimateRequestRecord request);

    Optional<EstimateRequestRecord> findRequestById(String requestId);
}
