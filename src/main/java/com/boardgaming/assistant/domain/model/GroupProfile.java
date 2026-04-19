package com.boardgaming.assistant.domain.model;

public record GroupProfile(
        int playerCount,
        GroupFamiliarity groupFamiliarity,
        TurnPace turnPace,
        AnalysisStyle analysisStyle,
        boolean childrenIncluded,
        String notes) {
}
