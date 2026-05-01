package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.PlayerCountFit;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimingEstimateResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<SessionTimingEstimate> parse(String estimateId, String gameId, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            int teachMinutes = requirePositiveInt(root, "teachMinutes");
            int playMinutes = requirePositiveInt(root, "playMinutes");
            int totalMinutes = requirePositiveInt(root, "totalMinutes");

            if (totalMinutes != teachMinutes + playMinutes) {
                return Optional.empty();
            }

            Confidence confidence = parseConfidence(requireString(root, "confidence"));
            List<PlayerCountFit> playerCountFit = parsePlayerCountFit(root);
            String explanation = requireString(root, "explanation");
            List<String> riskNotes = parseStringArray(root, "riskNotes");

            return Optional.of(new SessionTimingEstimate(
                    estimateId,
                    gameId,
                    teachMinutes,
                    playMinutes,
                    totalMinutes,
                    confidence,
                    playerCountFit,
                    explanation,
                    riskNotes));

        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private int requirePositiveInt(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isInt()) {
            throw new IllegalArgumentException("Missing or non-integer field: " + field);
        }
        int value = node.intValue();
        if (value <= 0) {
            throw new IllegalArgumentException("Field must be positive: " + field);
        }
        return value;
    }

    private String requireString(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Missing or non-string field: " + field);
        }
        return node.textValue();
    }

    private Confidence parseConfidence(String value) {
        return switch (value) {
            case "low" -> Confidence.LOW;
            case "medium" -> Confidence.MEDIUM;
            case "high" -> Confidence.HIGH;
            default -> throw new IllegalArgumentException("Invalid confidence: " + value);
        };
    }

    private Fit parseFit(String value) {
        return switch (value) {
            case "best" -> Fit.BEST;
            case "good" -> Fit.GOOD;
            case "ok" -> Fit.OK;
            case "not_recommended" -> Fit.NOT_RECOMMENDED;
            case "avoid" -> Fit.AVOID;
            default -> throw new IllegalArgumentException("Invalid fit: " + value);
        };
    }

    private List<PlayerCountFit> parsePlayerCountFit(JsonNode root) {
        JsonNode array = root.get("playerCountFit");
        if (array == null || !array.isArray()) {
            throw new IllegalArgumentException("Missing or non-array field: playerCountFit");
        }

        List<PlayerCountFit> result = new ArrayList<>();
        for (JsonNode entry : array) {
            int playerCount = requirePositiveInt(entry, "playerCount");
            Fit fit = parseFit(requireString(entry, "fit"));
            result.add(new PlayerCountFit(playerCount, fit));
        }
        return result;
    }

    private List<String> parseStringArray(JsonNode root, String field) {
        JsonNode array = root.get(field);
        if (array == null || !array.isArray()) {
            throw new IllegalArgumentException("Missing or non-array field: " + field);
        }

        List<String> result = new ArrayList<>();
        for (JsonNode element : array) {
            if (element.isTextual()) {
                result.add(element.textValue());
            }
        }
        return result;
    }
}
