package com.boardgaming.assistant.adapter.out.persistence;

import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.EstimateRequestPersistencePort;
import com.boardgaming.assistant.application.port.out.FeedbackPersistencePort;
import com.boardgaming.assistant.domain.model.EstimateRequestRecord;
import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryPersistenceAdapter implements EstimatePersistencePort,
        EstimateRequestPersistencePort, FeedbackPersistencePort {

    private final ConcurrentHashMap<String, SessionTimingEstimate> estimates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EstimateRequestRecord> requests = new ConcurrentHashMap<>();
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
    public void save(EstimateRequestRecord request) {
        requests.put(request.requestId(), request);
    }

    @Override
    public Optional<EstimateRequestRecord> findRequestById(String requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    @Override
    public void save(Feedback feedback) {
        feedbacks.put(feedback.feedbackId(), feedback);
    }

    @Override
    public Optional<Feedback> findFeedbackById(String feedbackId) {
        return Optional.ofNullable(feedbacks.get(feedbackId));
    }

    @Override
    public Optional<Feedback> findByEstimateId(String estimateId) {
        return feedbacks.values().stream()
                .filter(f -> f.estimateId().equals(estimateId))
                .findFirst();
    }
}
