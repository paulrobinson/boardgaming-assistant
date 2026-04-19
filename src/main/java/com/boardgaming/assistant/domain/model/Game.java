package com.boardgaming.assistant.domain.model;

import java.util.Map;

public record Game(
        String gameId,
        String barcode,
        String name,
        int minPlayers,
        int maxPlayers,
        int officialPlayTimeMinutes,
        int officialMinAge,
        double complexityWeight,
        Map<Integer, Fit> playerCountSummary,
        String notes) {
}
