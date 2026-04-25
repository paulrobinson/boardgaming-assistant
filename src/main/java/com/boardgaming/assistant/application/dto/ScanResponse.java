package com.boardgaming.assistant.application.dto;

public record ScanResponse(
        String gameId,
        String name,
        int officialPlayTimeMinutes,
        int minPlayers,
        int maxPlayers,
        boolean supported) {

    public static ScanResponse unsupported(String barcode) {
        return new ScanResponse(null, null, 0, 0, 0, false);
    }
}
