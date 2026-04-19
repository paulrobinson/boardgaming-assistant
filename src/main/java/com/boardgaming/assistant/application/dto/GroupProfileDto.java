package com.boardgaming.assistant.application.dto;

public record GroupProfileDto(
        int playerCount,
        String groupFamiliarity,
        String turnPace,
        String analysisStyle,
        boolean childrenIncluded,
        String notes) {
}
