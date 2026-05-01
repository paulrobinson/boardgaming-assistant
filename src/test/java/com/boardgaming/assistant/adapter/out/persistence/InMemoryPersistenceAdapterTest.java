package com.boardgaming.assistant.adapter.out.persistence;

import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.EstimateRequestRecord;
import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.GroupFamiliarity;
import com.boardgaming.assistant.domain.model.PlayerCountFit;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.model.TurnPace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryPersistenceAdapterTest {

    private InMemoryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryPersistenceAdapter();
    }

    // --- Estimate persistence ---

    @Test
    void savesAndFindsEstimateById() {
        var estimate = createEstimate("est_001");

        adapter.save(estimate);

        var found = adapter.findById("est_001");
        assertTrue(found.isPresent());
        assertEquals("est_001", found.get().estimateId());
        assertEquals(20, found.get().teachMinutes());
        assertEquals(70, found.get().playMinutes());
        assertEquals(90, found.get().totalMinutes());
    }

    @Test
    void returnsEmptyForUnknownEstimateId() {
        assertTrue(adapter.findById("nonexistent").isEmpty());
    }

    @Test
    void overwritesEstimateWithSameId() {
        adapter.save(createEstimate("est_001"));
        var updated = new SessionTimingEstimate("est_001", "catan",
                30, 80, 110, Confidence.HIGH,
                List.of(new PlayerCountFit(4, Fit.BEST)),
                "Updated explanation", List.of(), Instant.now());

        adapter.save(updated);

        var found = adapter.findById("est_001");
        assertTrue(found.isPresent());
        assertEquals(110, found.get().totalMinutes());
    }

    // --- Estimate request persistence ---

    @Test
    void savesAndFindsRequestById() {
        var request = createRequest("req_001", "est_001");

        adapter.save(request);

        var found = adapter.findRequestById("req_001");
        assertTrue(found.isPresent());
        assertEquals("req_001", found.get().requestId());
        assertEquals("est_001", found.get().estimateId());
        assertEquals("catan", found.get().gameId());
        assertEquals(4, found.get().playerCount());
    }

    @Test
    void returnsEmptyForUnknownRequestId() {
        assertTrue(adapter.findRequestById("nonexistent").isEmpty());
    }

    @Test
    void requestHasTimestamp() {
        var request = createRequest("req_001", "est_001");

        adapter.save(request);

        var found = adapter.findRequestById("req_001");
        assertTrue(found.isPresent());
        assertTrue(found.get().createdAt() != null);
    }

    // --- Feedback persistence ---

    @Test
    void savesAndFindsFeedbackById() {
        var feedback = createFeedback("fb_001", "est_001");

        adapter.save(feedback);

        var found = adapter.findFeedbackById("fb_001");
        assertTrue(found.isPresent());
        assertEquals("fb_001", found.get().feedbackId());
        assertEquals(25, found.get().actualTeachMinutes());
        assertEquals(80, found.get().actualPlayMinutes());
    }

    @Test
    void returnsEmptyForUnknownFeedbackId() {
        assertTrue(adapter.findFeedbackById("nonexistent").isEmpty());
    }

    @Test
    void findsFeedbackByEstimateId() {
        adapter.save(createFeedback("fb_001", "est_001"));
        adapter.save(createFeedback("fb_002", "est_002"));

        var found = adapter.findByEstimateId("est_001");

        assertTrue(found.isPresent());
        assertEquals("fb_001", found.get().feedbackId());
    }

    @Test
    void returnsEmptyFeedbackForUnknownEstimateId() {
        adapter.save(createFeedback("fb_001", "est_001"));

        assertTrue(adapter.findByEstimateId("est_999").isEmpty());
    }

    @Test
    void feedbackHasTimestamp() {
        var feedback = createFeedback("fb_001", "est_001");

        adapter.save(feedback);

        var found = adapter.findFeedbackById("fb_001");
        assertTrue(found.isPresent());
        assertTrue(found.get().createdAt() != null);
    }

    // --- helpers ---

    private SessionTimingEstimate createEstimate(String estimateId) {
        return new SessionTimingEstimate(estimateId, "catan",
                20, 70, 90, Confidence.MEDIUM,
                List.of(new PlayerCountFit(3, Fit.GOOD), new PlayerCountFit(4, Fit.BEST)),
                "Catan plays best at 4 players.", List.of("First play may run longer"),
                Instant.now());
    }

    private EstimateRequestRecord createRequest(String requestId, String estimateId) {
        return new EstimateRequestRecord(requestId, estimateId, "catan",
                4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE,
                false, null, Instant.now());
    }

    private Feedback createFeedback(String feedbackId, String estimateId) {
        return new Feedback(feedbackId, estimateId, 25, 80,
                "Took longer than expected", Instant.now());
    }
}
