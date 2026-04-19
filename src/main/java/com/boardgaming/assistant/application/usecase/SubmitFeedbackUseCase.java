package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.FeedbackRequest;
import com.boardgaming.assistant.application.dto.FeedbackResponse;
import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.FeedbackPersistencePort;
import com.boardgaming.assistant.domain.model.Feedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class SubmitFeedbackUseCase {

    private final EstimatePersistencePort estimatePersistence;
    private final FeedbackPersistencePort feedbackPersistence;
    private final AnalyticsPort analytics;

    @Inject
    public SubmitFeedbackUseCase(
            EstimatePersistencePort estimatePersistence,
            FeedbackPersistencePort feedbackPersistence,
            AnalyticsPort analytics) {
        this.estimatePersistence = estimatePersistence;
        this.feedbackPersistence = feedbackPersistence;
        this.analytics = analytics;
    }

    public FeedbackResponse execute(FeedbackRequest request) {
        var estimate = estimatePersistence.findById(request.estimateId());
        if (estimate.isEmpty()) {
            return null;
        }

        String feedbackId = "fb_" + UUID.randomUUID().toString().substring(0, 8);
        Feedback feedback = new Feedback(
                feedbackId,
                request.estimateId(),
                request.actualTeachMinutes(),
                request.actualPlayMinutes(),
                request.notes());

        feedbackPersistence.save(feedback);
        analytics.recordFeedbackReceived(feedback);

        return new FeedbackResponse(feedbackId, request.estimateId(), true);
    }
}
