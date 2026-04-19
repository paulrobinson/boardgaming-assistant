package com.boardgaming.assistant.application.dto;

public record EstimateRequest(
        String gameId,
        GroupProfileDto groupProfile) {
}
