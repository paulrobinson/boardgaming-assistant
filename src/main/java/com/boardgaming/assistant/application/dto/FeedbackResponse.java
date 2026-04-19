package com.boardgaming.assistant.application.dto;

public record FeedbackResponse(
        String feedbackId,
        String estimateId,
        boolean accepted) {
}
