package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.FeedbackRequest;
import com.boardgaming.assistant.application.dto.FeedbackResponse;
import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.EventSinkPort;
import com.boardgaming.assistant.application.port.out.FeedbackPersistencePort;
import com.boardgaming.assistant.domain.model.Event;
import com.boardgaming.assistant.domain.model.EventType;
import com.boardgaming.assistant.domain.model.Feedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SubmitFeedbackUseCase {

    private static final Logger LOG = Logger.getLogger(SubmitFeedbackUseCase.class.getName());

    private final EstimatePersistencePort estimatePersistence;
    private final FeedbackPersistencePort feedbackPersistence;
    private final AnalyticsPort analytics;
    private final EventSinkPort eventSink;

    @Inject
    public SubmitFeedbackUseCase(
            EstimatePersistencePort estimatePersistence,
            FeedbackPersistencePort feedbackPersistence,
            AnalyticsPort analytics,
            EventSinkPort eventSink) {
        this.estimatePersistence = estimatePersistence;
        this.feedbackPersistence = feedbackPersistence;
        this.analytics = analytics;
        this.eventSink = eventSink;
    }

    public FeedbackResponse execute(FeedbackRequest request) {
        if (request.actualTeachMinutes() <= 0) {
            throw new IllegalArgumentException("actualTeachMinutes must be greater than zero");
        }
        if (request.actualPlayMinutes() <= 0) {
            throw new IllegalArgumentException("actualPlayMinutes must be greater than zero");
        }

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
                request.notes(),
                Instant.now());

        feedbackPersistence.save(feedback);
        analytics.recordFeedbackReceived(feedback);
        publishSafely(Event.of(EventType.FEEDBACK_SUBMITTED, Map.of(
                "feedbackId", feedbackId,
                "estimateId", request.estimateId(),
                "actualTeachMinutes", String.valueOf(request.actualTeachMinutes()),
                "actualPlayMinutes", String.valueOf(request.actualPlayMinutes()))));

        return new FeedbackResponse(feedbackId, request.estimateId(), true);
    }

    private void publishSafely(Event event) {
        try {
            eventSink.publish(event);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to publish event: " + event.type(), e);
        }
    }
}
