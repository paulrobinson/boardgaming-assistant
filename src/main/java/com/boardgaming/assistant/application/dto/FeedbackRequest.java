package com.boardgaming.assistant.application.dto;

public record FeedbackRequest(
        String estimateId,
        int actualTeachMinutes,
        int actualPlayMinutes,
        String notes) {
}
