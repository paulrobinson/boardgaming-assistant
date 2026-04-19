package com.boardgaming.assistant.domain.model;

public record Feedback(
        String feedbackId,
        String estimateId,
        int actualTeachMinutes,
        int actualPlayMinutes,
        String notes) {
}
