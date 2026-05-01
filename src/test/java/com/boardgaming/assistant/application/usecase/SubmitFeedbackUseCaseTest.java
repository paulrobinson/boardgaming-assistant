package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.FeedbackRequest;
import com.boardgaming.assistant.application.dto.FeedbackResponse;
import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.FeedbackPersistencePort;
import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.PlayerCountFit;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitFeedbackUseCaseTest {

    private SubmitFeedbackUseCase useCase;
    private FakeEstimatePersistence estimatePersistence;
    private FakeFeedbackPersistence feedbackPersistence;
    private FakeAnalytics fakeAnalytics;

    private static final SessionTimingEstimate EXISTING_ESTIMATE = new SessionTimingEstimate(
            "est_abc123", "catan", 20, 70, 90, Confidence.MEDIUM,
            List.of(new PlayerCountFit(3, Fit.GOOD), new PlayerCountFit(4, Fit.BEST)),
            "Catan plays well at 4 players.", List.of("First play may run longer"),
            Instant.now());

    @BeforeEach
    void setUp() {
        estimatePersistence = new FakeEstimatePersistence();
        estimatePersistence.save(EXISTING_ESTIMATE);
        feedbackPersistence = new FakeFeedbackPersistence();
        fakeAnalytics = new FakeAnalytics();

        useCase = new SubmitFeedbackUseCase(estimatePersistence, feedbackPersistence, fakeAnalytics);
    }

    @Test
    void submitsFeedbackForExistingEstimate() {
        var request = new FeedbackRequest("est_abc123", 25, 80, "Took a bit longer than expected");

        FeedbackResponse result = useCase.execute(request);

        assertNotNull(result);
        assertNotNull(result.feedbackId());
        assertEquals("est_abc123", result.estimateId());
        assertTrue(result.accepted());
    }

    @Test
    void persistsFeedback() {
        var request = new FeedbackRequest("est_abc123", 25, 80, null);

        FeedbackResponse result = useCase.execute(request);

        assertTrue(feedbackPersistence.stored.containsKey(result.feedbackId()));
        Feedback stored = feedbackPersistence.stored.get(result.feedbackId());
        assertEquals(25, stored.actualTeachMinutes());
        assertEquals(80, stored.actualPlayMinutes());
    }

    @Test
    void recordsAnalytics() {
        var request = new FeedbackRequest("est_abc123", 25, 80, null);

        useCase.execute(request);

        assertEquals(1, fakeAnalytics.feedbackEvents.size());
    }

    @Test
    void feedbackHasTimestamp() {
        var request = new FeedbackRequest("est_abc123", 25, 80, null);

        FeedbackResponse result = useCase.execute(request);

        Feedback stored = feedbackPersistence.stored.get(result.feedbackId());
        assertNotNull(stored.createdAt());
    }

    @Test
    void returnsNullForUnknownEstimate() {
        var request = new FeedbackRequest("est_nonexistent", 25, 80, null);

        FeedbackResponse result = useCase.execute(request);

        assertNull(result);
    }

    @Test
    void rejectsZeroActualTeachMinutes() {
        var request = new FeedbackRequest("est_abc123", 0, 80, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
    }

    @Test
    void rejectsNegativeActualTeachMinutes() {
        var request = new FeedbackRequest("est_abc123", -5, 80, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
    }

    @Test
    void rejectsZeroActualPlayMinutes() {
        var request = new FeedbackRequest("est_abc123", 25, 0, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
    }

    @Test
    void rejectsNegativeActualPlayMinutes() {
        var request = new FeedbackRequest("est_abc123", 25, -10, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
    }

    // --- Test doubles ---

    static class FakeEstimatePersistence implements EstimatePersistencePort {
        private final Map<String, SessionTimingEstimate> store = new ConcurrentHashMap<>();

        @Override
        public void save(SessionTimingEstimate estimate) {
            store.put(estimate.estimateId(), estimate);
        }

        @Override
        public Optional<SessionTimingEstimate> findById(String estimateId) {
            return Optional.ofNullable(store.get(estimateId));
        }
    }

    static class FakeFeedbackPersistence implements FeedbackPersistencePort {
        final Map<String, Feedback> stored = new ConcurrentHashMap<>();

        @Override
        public void save(Feedback feedback) {
            stored.put(feedback.feedbackId(), feedback);
        }

        @Override
        public Optional<Feedback> findFeedbackById(String feedbackId) {
            return Optional.ofNullable(stored.get(feedbackId));
        }

        @Override
        public Optional<Feedback> findByEstimateId(String estimateId) {
            return stored.values().stream()
                    .filter(f -> f.estimateId().equals(estimateId))
                    .findFirst();
        }
    }

    static class FakeAnalytics implements AnalyticsPort {
        final List<SessionTimingEstimate> estimateEvents = new ArrayList<>();
        final List<Feedback> feedbackEvents = new ArrayList<>();

        @Override
        public void recordEstimateCreated(SessionTimingEstimate estimate) {
            estimateEvents.add(estimate);
        }

        @Override
        public void recordFeedbackReceived(Feedback feedback) {
            feedbackEvents.add(feedback);
        }
    }
}
