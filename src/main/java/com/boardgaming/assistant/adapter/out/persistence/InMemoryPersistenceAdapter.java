package com.boardgaming.assistant.adapter.out.persistence;

import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.FeedbackPersistencePort;
import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryPersistenceAdapter implements EstimatePersistencePort, FeedbackPersistencePort {

    private final ConcurrentHashMap<String, SessionTimingEstimate> estimates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Feedback> feedbacks = new ConcurrentHashMap<>();

    @Override
    public void save(SessionTimingEstimate estimate) {
        estimates.put(estimate.estimateId(), estimate);
    }

    @Override
    public Optional<SessionTimingEstimate> findById(String estimateId) {
        return Optional.ofNullable(estimates.get(estimateId));
    }

    @Override
    public void save(Feedback feedback) {
        feedbacks.put(feedback.feedbackId(), feedback);
    }
}
