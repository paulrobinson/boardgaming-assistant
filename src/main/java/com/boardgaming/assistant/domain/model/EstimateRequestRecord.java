package com.boardgaming.assistant.domain.model;

import java.time.Instant;

public record EstimateRequestRecord(
        String requestId,
        String estimateId,
        String gameId,
        int playerCount,
        GroupFamiliarity groupFamiliarity,
        TurnPace turnPace,
        AnalysisStyle analysisStyle,
        boolean childrenIncluded,
        String notes,
        Instant createdAt) {
}
