package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.SessionTimingEstimate;

import java.util.Optional;

public interface EstimatePersistencePort {
    void save(SessionTimingEstimate estimate);

    Optional<SessionTimingEstimate> findById(String estimateId);
}
