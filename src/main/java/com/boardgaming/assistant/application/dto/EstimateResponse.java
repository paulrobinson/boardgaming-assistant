package com.boardgaming.assistant.application.dto;

import java.util.List;

public record EstimateResponse(
        String estimateId,
        int teachMinutes,
        int playMinutes,
        int totalMinutes,
        String confidence,
        List<PlayerCountFitDto> playerCountFit,
        String explanation,
        List<String> riskNotes) {
}
