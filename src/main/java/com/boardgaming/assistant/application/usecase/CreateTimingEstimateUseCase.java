package com.boardgaming.assistant.application.usecase;

import com.boardgaming.assistant.application.dto.EstimateRequest;
import com.boardgaming.assistant.application.dto.EstimateResponse;
import com.boardgaming.assistant.application.dto.PlayerCountFitDto;
import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.application.port.out.EstimateCachePort;
import com.boardgaming.assistant.application.port.out.EstimatePersistencePort;
import com.boardgaming.assistant.application.port.out.GameCatalogPort;
import com.boardgaming.assistant.application.port.out.TimingEstimateModelPort;
import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupFamiliarity;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.model.TurnPace;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CreateTimingEstimateUseCase {

    private final GameCatalogPort gameCatalog;
    private final TimingEstimateModelPort timingModel;
    private final EstimatePersistencePort persistence;
    private final EstimateCachePort cache;
    private final AnalyticsPort analytics;

    @Inject
    public CreateTimingEstimateUseCase(
            GameCatalogPort gameCatalog,
            TimingEstimateModelPort timingModel,
            EstimatePersistencePort persistence,
            EstimateCachePort cache,
            AnalyticsPort analytics) {
        this.gameCatalog = gameCatalog;
        this.timingModel = timingModel;
        this.persistence = persistence;
        this.cache = cache;
        this.analytics = analytics;
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
            return toResponse(cached.get());
        }

        String estimateId = "est_" + UUID.randomUUID().toString().substring(0, 8);
        SessionTimingEstimate estimate = timingModel.generate(estimateId, game, profile);

        persistence.save(estimate);
        cache.put(cacheKey, estimate);
        analytics.recordEstimateCreated(estimate);

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
        var gp = request.groupProfile();
        return request.gameId()
                + ":" + gp.playerCount()
                + ":" + gp.groupFamiliarity()
                + ":" + gp.turnPace()
                + ":" + gp.analysisStyle()
                + ":" + gp.childrenIncluded();
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
