package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupFamiliarity;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.model.TurnPace;
import com.boardgaming.assistant.domain.service.TimingHeuristicsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmTimingEstimateModelAdapterTest {

    private TimingHeuristicsService heuristics;

    private static final Game CATAN = new Game(
            "catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
            Map.of(3, Fit.GOOD, 4, Fit.BEST), "notes");

    private static final GroupProfile MIXED_GROUP = new GroupProfile(
            4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE,
            false, null);

    private static final String DETERMINISTIC_JSON = """
            {
              "teachMinutes": 20,
              "playMinutes": 70,
              "totalMinutes": 90,
              "confidence": "medium",
              "playerCountFit": [
                {"playerCount": 3, "fit": "good"},
                {"playerCount": 4, "fit": "best"}
              ],
              "explanation": "Catan plays best at 4 players with a mixed group.",
              "riskNotes": ["Rules reminders may increase downtime"]
            }
            """;

    @BeforeEach
    void setUp() {
        heuristics = new TimingHeuristicsService();
    }

    @Test
    void returnsLlmEstimateOnValidResponse() {
        LlmClient fakeLlm = (system, user) -> DETERMINISTIC_JSON;
        var adapter = new LlmTimingEstimateModelAdapter(fakeLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);

        assertEquals(20, result.teachMinutes());
        assertEquals(70, result.playMinutes());
        assertEquals(90, result.totalMinutes());
        assertEquals(Confidence.MEDIUM, result.confidence());
        assertEquals("Catan plays best at 4 players with a mixed group.", result.explanation());
    }

    @Test
    void fallsBackToHeuristicsOnInvalidJson() {
        LlmClient fakeLlm = (system, user) -> "this is not json";
        var adapter = new LlmTimingEstimateModelAdapter(fakeLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);
        SessionTimingEstimate expected = heuristics.calculate("est_001", CATAN, MIXED_GROUP);

        assertNotNull(result);
        assertEquals(expected.teachMinutes(), result.teachMinutes());
        assertEquals(expected.playMinutes(), result.playMinutes());
        assertEquals(expected.totalMinutes(), result.totalMinutes());
    }

    @Test
    void fallsBackToHeuristicsOnTotalMismatch() {
        String badTotalJson = """
                {
                  "teachMinutes": 20,
                  "playMinutes": 70,
                  "totalMinutes": 999,
                  "confidence": "medium",
                  "playerCountFit": [{"playerCount": 4, "fit": "best"}],
                  "explanation": "test",
                  "riskNotes": []
                }
                """;
        LlmClient fakeLlm = (system, user) -> badTotalJson;
        var adapter = new LlmTimingEstimateModelAdapter(fakeLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);
        SessionTimingEstimate expected = heuristics.calculate("est_001", CATAN, MIXED_GROUP);

        assertEquals(expected.teachMinutes(), result.teachMinutes());
        assertEquals(expected.playMinutes(), result.playMinutes());
        assertEquals(expected.totalMinutes(), result.totalMinutes());
    }

    @Test
    void fallsBackToHeuristicsOnLlmException() {
        LlmClient failingLlm = (system, user) -> {
            throw new RuntimeException("LLM service unavailable");
        };
        var adapter = new LlmTimingEstimateModelAdapter(failingLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);
        SessionTimingEstimate expected = heuristics.calculate("est_001", CATAN, MIXED_GROUP);

        assertNotNull(result);
        assertEquals(expected.teachMinutes(), result.teachMinutes());
        assertEquals(expected.playMinutes(), result.playMinutes());
        assertEquals(expected.totalMinutes(), result.totalMinutes());
    }

    @Test
    void fallbackAlwaysSatisfiesTotalMinutesInvariant() {
        LlmClient failingLlm = (system, user) -> {
            throw new RuntimeException("boom");
        };
        var adapter = new LlmTimingEstimateModelAdapter(failingLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);

        assertEquals(result.teachMinutes() + result.playMinutes(), result.totalMinutes());
    }

    @Test
    void llmResultPreservedWhenValid() {
        LlmClient fakeLlm = (system, user) -> DETERMINISTIC_JSON;
        var adapter = new LlmTimingEstimateModelAdapter(fakeLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);

        assertEquals(2, result.playerCountFit().size());
        assertEquals(Fit.GOOD, result.playerCountFit().get(0).fit());
        assertEquals(Fit.BEST, result.playerCountFit().get(1).fit());
        assertEquals(1, result.riskNotes().size());
        assertTrue(result.riskNotes().get(0).contains("Rules reminders"));
    }

    @Test
    void fallsBackOnEmptyResponse() {
        LlmClient fakeLlm = (system, user) -> "";
        var adapter = new LlmTimingEstimateModelAdapter(fakeLlm, heuristics);

        SessionTimingEstimate result = adapter.generate("est_001", CATAN, MIXED_GROUP);
        SessionTimingEstimate expected = heuristics.calculate("est_001", CATAN, MIXED_GROUP);

        assertNotNull(result);
        assertEquals(expected.totalMinutes(), result.totalMinutes());
    }
}
