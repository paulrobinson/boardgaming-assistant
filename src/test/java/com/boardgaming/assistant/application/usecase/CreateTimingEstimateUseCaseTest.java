package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.EstimateRequest;
import com.boardgaming.assistant.application.dto.EstimateResponse;
import com.boardgaming.assistant.application.dto.GroupProfileDto;
import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.application.port.out.EstimateCachePort;
import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.GameCatalogPort;
import com.boardgaming.assistant.application.port.out.TimingEstimateModelPort;
import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.service.TimingHeuristicsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTimingEstimateUseCaseTest {

    private CreateTimingEstimateUseCase useCase;
    private FakeGameCatalog fakeCatalog;
    private FakePersistence fakePersistence;
    private FakeCache fakeCache;
    private FakeAnalytics fakeAnalytics;

    private static final Game CATAN = new Game(
            "catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
            Map.of(3, Fit.GOOD, 4, Fit.BEST), "notes");

    @BeforeEach
    void setUp() {
        fakeCatalog = new FakeGameCatalog();
        fakeCatalog.add(CATAN);
        var fakeModel = new FakeTimingModel();
        fakePersistence = new FakePersistence();
        fakeCache = new FakeCache();
        fakeAnalytics = new FakeAnalytics();

        useCase = new CreateTimingEstimateUseCase(
                fakeCatalog, fakeModel, fakePersistence, fakeCache, fakeAnalytics);
    }

    @Test
    void createsEstimateForKnownGame() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        EstimateResponse result = useCase.execute(request);

        assertNotNull(result);
        assertNotNull(result.estimateId());
        assertTrue(result.teachMinutes() > 0);
        assertTrue(result.playMinutes() > 0);
        assertEquals(result.teachMinutes() + result.playMinutes(), result.totalMinutes());
    }

    @Test
    void totalMinutesAlwaysEqualsSumOfTeachAndPlay() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "new", "slow", "high", true, "first time"));

        EstimateResponse result = useCase.execute(request);

        assertEquals(result.teachMinutes() + result.playMinutes(), result.totalMinutes());
    }

    @Test
    void returnsNullForUnknownGame() {
        var request = new EstimateRequest("unknown-game",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        EstimateResponse result = useCase.execute(request);

        assertNull(result);
    }

    @Test
    void persistsEstimate() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        EstimateResponse result = useCase.execute(request);

        assertTrue(fakePersistence.findById(result.estimateId()).isPresent());
    }

    @Test
    void recordsAnalytics() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        useCase.execute(request);

        assertEquals(1, fakeAnalytics.estimateEvents.size());
    }

    @Test
    void returnsCachedEstimateOnRepeatRequest() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        EstimateResponse first = useCase.execute(request);
        EstimateResponse second = useCase.execute(request);

        assertEquals(first.estimateId(), second.estimateId());
        assertEquals(1, fakeAnalytics.estimateEvents.size());
    }

    @Test
    void responseContainsPlayerCountFit() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        EstimateResponse result = useCase.execute(request);

        assertNotNull(result.playerCountFit());
        assertEquals(2, result.playerCountFit().size());
    }

    @Test
    void responseContainsExplanation() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "mixed", "average", "moderate", false, null));

        EstimateResponse result = useCase.execute(request);

        assertNotNull(result.explanation());
        assertTrue(result.explanation().contains("Catan"));
    }

    @Test
    void confidenceIsLowercaseString() {
        var request = new EstimateRequest("catan",
                new GroupProfileDto(4, "experienced", "average", "low", false, null));

        EstimateResponse result = useCase.execute(request);

        assertTrue(result.confidence().equals("high")
                || result.confidence().equals("medium")
                || result.confidence().equals("low"));
    }

    // --- Test doubles ---

    static class FakeGameCatalog implements GameCatalogPort {
        private final Map<String, Game> games = new ConcurrentHashMap<>();

        void add(Game game) {
            games.put(game.gameId(), game);
        }

        @Override
        public Optional<Game> findByGameId(String gameId) {
            return Optional.ofNullable(games.get(gameId));
        }
    }

    static class FakeTimingModel implements TimingEstimateModelPort {
        private final TimingHeuristicsService heuristics = new TimingHeuristicsService();

        @Override
        public SessionTimingEstimate generate(String estimateId, Game game, GroupProfile profile) {
            return heuristics.calculate(estimateId, game, profile);
        }
    }

    static class FakePersistence implements EstimatePersistencePort {
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

    static class FakeCache implements EstimateCachePort {
        private final Map<String, SessionTimingEstimate> store = new ConcurrentHashMap<>();

        @Override
        public Optional<SessionTimingEstimate> get(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void put(String key, SessionTimingEstimate estimate) {
            store.put(key, estimate);
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
