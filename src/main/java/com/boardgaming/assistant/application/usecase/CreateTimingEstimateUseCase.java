package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.adapter.out.cache.EstimateCacheKeyBuilder;
import com.boardgaming.assistant.application.dto.EstimateRequest;
import com.boardgaming.assistant.application.dto.EstimateResponse;
import com.boardgaming.assistant.application.dto.PlayerCountFitDto;
import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.application.port.out.EstimateCachePort;
import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.EstimateRequestPersistencePort;
import com.boardgaming.assistant.application.port.out.EventSinkPort;
import com.boardgaming.assistant.application.port.out.GameCatalogPort;
import com.boardgaming.assistant.application.port.out.TimingEstimateModelPort;
import com.boardgaming.assistant.domain.model.Event;
import com.boardgaming.assistant.domain.model.EventType;
import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.EstimateRequestRecord;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupFamiliarity;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.model.TurnPace;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CreateTimingEstimateUseCase {

    private static final Logger LOG = Logger.getLogger(CreateTimingEstimateUseCase.class.getName());
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final GameCatalogPort gameCatalog;
    private final TimingEstimateModelPort timingModel;
    private final EstimatePersistencePort persistence;
    private final EstimateRequestPersistencePort requestPersistence;
    private final EstimateCachePort cache;
    private final AnalyticsPort analytics;
    private final EventSinkPort eventSink;
    private final EstimateCacheKeyBuilder cacheKeyBuilder = new EstimateCacheKeyBuilder();

    @Inject
    public CreateTimingEstimateUseCase(
            GameCatalogPort gameCatalog,
            TimingEstimateModelPort timingModel,
            EstimatePersistencePort persistence,
            EstimateRequestPersistencePort requestPersistence,
            EstimateCachePort cache,
            AnalyticsPort analytics,
            EventSinkPort eventSink) {
        this.gameCatalog = gameCatalog;
        this.timingModel = timingModel;
        this.persistence = persistence;
        this.requestPersistence = requestPersistence;
        this.cache = cache;
        this.analytics = analytics;
        this.eventSink = eventSink;
    }

    public EstimateResponse execute(EstimateRequest request) {
        Game game = gameCatalog.findByGameId(request.gameId())
                .orElse(null);

        if (game == null) {
            return null;
        }

        GroupProfile profile = toGroupProfile(request);
        String cacheKey = buildCacheKey(request);

        var cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            publishSafely(Event.of(EventType.ESTIMATE_CACHE_HIT, Map.of(
                    "estimateId", cached.get().estimateId(),
                    "gameId", cached.get().gameId(),
                    "cacheKey", cacheKey)));
            return toResponse(cached.get());
        }

        String estimateId = "est_" + UUID.randomUUID().toString().substring(0, 8);
        String requestId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        EstimateRequestRecord requestRecord = new EstimateRequestRecord(
                requestId,
                estimateId,
                request.gameId(),
                profile.playerCount(),
                profile.groupFamiliarity(),
                profile.turnPace(),
                profile.analysisStyle(),
                profile.childrenIncluded(),
                profile.notes(),
                Instant.now());
        requestPersistence.save(requestRecord);

        SessionTimingEstimate estimate = timingModel.generate(estimateId, game, profile);

        persistence.save(estimate);
        cache.put(cacheKey, estimate, CACHE_TTL);
        analytics.recordEstimateCreated(estimate);
        publishSafely(Event.of(EventType.ESTIMATE_CREATED, Map.of(
                "estimateId", estimate.estimateId(),
                "gameId", estimate.gameId(),
                "teachMinutes", String.valueOf(estimate.teachMinutes()),
                "playMinutes", String.valueOf(estimate.playMinutes()),
                "totalMinutes", String.valueOf(estimate.totalMinutes()),
                "confidence", estimate.confidence().name().toLowerCase())));

        return toResponse(estimate);
    }

    private GroupProfile toGroupProfile(EstimateRequest request) {
        var dto = request.groupProfile();
        return new GroupProfile(
                dto.playerCount(),
                GroupFamiliarity.valueOf(dto.groupFamiliarity().toUpperCase()),
                TurnPace.valueOf(dto.turnPace().toUpperCase()),
                AnalysisStyle.valueOf(dto.analysisStyle().toUpperCase()),
                dto.childrenIncluded(),
                dto.notes());
    }

    private String buildCacheKey(EstimateRequest request) {
        return cacheKeyBuilder.build(request.gameId(), request.groupProfile());
    }

    private void publishSafely(Event event) {
        try {
            eventSink.publish(event);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to publish event: " + event.type(), e);
        }
    }

    private EstimateResponse toResponse(SessionTimingEstimate est) {
        var fitDtos = est.playerCountFit().stream()
                .map(f -> new PlayerCountFitDto(f.playerCount(), f.fit().name().toLowerCase()))
                .toList();

        return new EstimateResponse(
                est.estimateId(),
                est.teachMinutes(),
                est.playMinutes(),
                est.totalMinutes(),
                est.confidence().name().toLowerCase(),
                fitDtos,
                est.explanation(),
                est.riskNotes());
    }
}
