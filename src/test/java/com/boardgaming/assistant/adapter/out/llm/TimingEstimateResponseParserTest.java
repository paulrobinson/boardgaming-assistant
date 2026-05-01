package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingEstimateResponseParserTest {

    private TimingEstimateResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new TimingEstimateResponseParser();
    }

    private static final String VALID_JSON = """
            {
              "teachMinutes": 24,
              "playMinutes": 66,
              "totalMinutes": 90,
              "confidence": "medium",
              "playerCountFit": [
                {"playerCount": 3, "fit": "good"},
                {"playerCount": 4, "fit": "best"}
              ],
              "explanation": "Catan plays best at 4 players.",
              "riskNotes": ["Rules reminders may increase downtime"]
            }
            """;

    @Test
    void parsesValidJson() {
        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", VALID_JSON);

        assertTrue(result.isPresent());
        SessionTimingEstimate estimate = result.get();
        assertEquals("est_001", estimate.estimateId());
        assertEquals("catan", estimate.gameId());
        assertEquals(24, estimate.teachMinutes());
        assertEquals(66, estimate.playMinutes());
        assertEquals(90, estimate.totalMinutes());
        assertEquals(Confidence.MEDIUM, estimate.confidence());
        assertEquals("Catan plays best at 4 players.", estimate.explanation());
        assertEquals(1, estimate.riskNotes().size());
    }

    @Test
    void parsesPlayerCountFit() {
        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", VALID_JSON);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().playerCountFit().size());
        assertEquals(3, result.get().playerCountFit().get(0).playerCount());
        assertEquals(Fit.GOOD, result.get().playerCountFit().get(0).fit());
        assertEquals(4, result.get().playerCountFit().get(1).playerCount());
        assertEquals(Fit.BEST, result.get().playerCountFit().get(1).fit());
    }

    @Test
    void totalMinutesMustEqualSum() {
        String json = """
                {
                  "teachMinutes": 24,
                  "playMinutes": 66,
                  "totalMinutes": 100,
                  "confidence": "medium",
                  "playerCountFit": [{"playerCount": 3, "fit": "good"}],
                  "explanation": "test",
                  "riskNotes": []
                }
                """;

        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsInvalidJson() {
        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", "not json at all");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsEmptyJsonObject() {
        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", "{}");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsMissingFields() {
        String json = """
                {
                  "teachMinutes": 24,
                  "playMinutes": 66,
                  "totalMinutes": 90
                }
                """;

        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsZeroTeachMinutes() {
        String json = """
                {
                  "teachMinutes": 0,
                  "playMinutes": 66,
                  "totalMinutes": 66,
                  "confidence": "medium",
                  "playerCountFit": [{"playerCount": 3, "fit": "good"}],
                  "explanation": "test",
                  "riskNotes": []
                }
                """;

        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsNegativePlayMinutes() {
        String json = """
                {
                  "teachMinutes": 24,
                  "playMinutes": -10,
                  "totalMinutes": 14,
                  "confidence": "medium",
                  "playerCountFit": [{"playerCount": 3, "fit": "good"}],
                  "explanation": "test",
                  "riskNotes": []
                }
                """;

        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsInvalidConfidence() {
        String json = """
                {
                  "teachMinutes": 24,
                  "playMinutes": 66,
                  "totalMinutes": 90,
                  "confidence": "very_high",
                  "playerCountFit": [{"playerCount": 3, "fit": "good"}],
                  "explanation": "test",
                  "riskNotes": []
                }
                """;

        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsInvalidFitValue() {
        String json = """
                {
                  "teachMinutes": 24,
                  "playMinutes": 66,
                  "totalMinutes": 90,
                  "confidence": "medium",
                  "playerCountFit": [{"playerCount": 3, "fit": "perfect"}],
                  "explanation": "test",
                  "riskNotes": []
                }
                """;

        Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

        assertTrue(result.isEmpty());
    }

    @Test
    void parsesAllConfidenceLevels() {
        for (String level : new String[]{"low", "medium", "high"}) {
            String json = """
                    {
                      "teachMinutes": 10,
                      "playMinutes": 50,
                      "totalMinutes": 60,
                      "confidence": "%s",
                      "playerCountFit": [{"playerCount": 3, "fit": "good"}],
                      "explanation": "test",
                      "riskNotes": []
                    }
                    """.formatted(level);

            Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

            assertTrue(result.isPresent(), "Should parse confidence level: " + level);
        }
    }

    @Test
    void parsesAllFitValues() {
        for (String fit : new String[]{"best", "good", "ok", "not_recommended", "avoid"}) {
            String json = """
                    {
                      "teachMinutes": 10,
                      "playMinutes": 50,
                      "totalMinutes": 60,
                      "confidence": "medium",
                      "playerCountFit": [{"playerCount": 3, "fit": "%s"}],
                      "explanation": "test",
                      "riskNotes": []
                    }
                    """.formatted(fit);

            Optional<SessionTimingEstimate> result = parser.parse("est_001", "catan", json);

            assertTrue(result.isPresent(), "Should parse fit value: " + fit);
        }
    }
}
