package com.boardgaming.assistant.domain.model;

import java.time.Instant;

public record Feedback(
        String feedbackId,
        String estimateId,
        int actualTeachMinutes,
        int actualPlayMinutes,
        String notes,
        Instant createdAt) {
}
