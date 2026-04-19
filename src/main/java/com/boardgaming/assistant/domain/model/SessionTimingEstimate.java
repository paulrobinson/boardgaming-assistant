package com.boardgaming.assistant.domain.model;

import java.util.List;

public record SessionTimingEstimate(
        String estimateId,
        String gameId,
        int teachMinutes,
        int playMinutes,
        int totalMinutes,
        Confidence confidence,
        List<PlayerCountFit> playerCountFit,
        String explanation,
        List<String> riskNotes) {
}
