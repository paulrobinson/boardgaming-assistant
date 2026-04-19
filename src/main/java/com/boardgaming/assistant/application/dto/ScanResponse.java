package com.boardgaming.assistant.application.dto;

public record ScanResponse(
        String gameId,
        String name,
        int officialPlayTimeMinutes,
        int minPlayers,
        int maxPlayers,
        boolean supported) {
}
