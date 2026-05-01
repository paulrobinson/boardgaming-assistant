package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Feedback;

import java.util.Optional;

public interface FeedbackPersistencePort {
    void save(Feedback feedback);

    Optional<Feedback> findFeedbackById(String feedbackId);

    Optional<Feedback> findByEstimateId(String estimateId);
}
