package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupFamiliarity;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.TurnPace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingEstimatePromptBuilderTest {

    private TimingEstimatePromptBuilder builder;

    private static final Game CATAN = new Game(
            "catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
            Map.of(3, Fit.GOOD, 4, Fit.BEST), "Trade and build");

    @BeforeEach
    void setUp() {
        builder = new TimingEstimatePromptBuilder();
    }

    @Test
    void systemPromptContainsJsonFormat() {
        String prompt = builder.buildSystemPrompt();

        assertTrue(prompt.contains("teachMinutes"));
        assertTrue(prompt.contains("playMinutes"));
        assertTrue(prompt.contains("totalMinutes"));
        assertTrue(prompt.contains("confidence"));
        assertTrue(prompt.contains("playerCountFit"));
        assertTrue(prompt.contains("explanation"));
        assertTrue(prompt.contains("riskNotes"));
    }

    @Test
    void systemPromptEnforcesTotalMinutesInvariant() {
        String prompt = builder.buildSystemPrompt();

        assertTrue(prompt.contains("totalMinutes MUST equal teachMinutes + playMinutes"));
    }

    @Test
    void systemPromptClarifiesEstimateMeansTime() {
        String prompt = builder.buildSystemPrompt();

        assertTrue(prompt.contains("time estimate in minutes"));
        assertTrue(prompt.contains("never a price or cost"));
    }

    @Test
    void userPromptContainsGameMetadata() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED,
                TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        String prompt = builder.buildUserPrompt(CATAN, profile);

        assertTrue(prompt.contains("Catan"));
        assertTrue(prompt.contains("60 minutes"));
        assertTrue(prompt.contains("3-4"));
        assertTrue(prompt.contains("2.3"));
    }

    @Test
    void userPromptContainsGroupProfile() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.NEW,
                TurnPace.SLOW, AnalysisStyle.HIGH, true, "first time playing");

        String prompt = builder.buildUserPrompt(CATAN, profile);

        assertTrue(prompt.contains("Player count: 4"));
        assertTrue(prompt.contains("new"));
        assertTrue(prompt.contains("slow"));
        assertTrue(prompt.contains("high"));
        assertTrue(prompt.contains("Children included: true"));
        assertTrue(prompt.contains("first time playing"));
    }

    @Test
    void userPromptContainsPlayerCountFit() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED,
                TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        String prompt = builder.buildUserPrompt(CATAN, profile);

        assertTrue(prompt.contains("Player count fit:"));
        assertTrue(prompt.contains("best"));
        assertTrue(prompt.contains("good"));
    }

    @Test
    void userPromptOmitsNotesWhenNull() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED,
                TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        Game gameNoNotes = new Game(
                "catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
                Map.of(3, Fit.GOOD, 4, Fit.BEST), null);

        String prompt = builder.buildUserPrompt(gameNoNotes, profile);

        assertFalse(prompt.contains("Notes:"));
    }

    @Test
    void userPromptIncludesGameNotes() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED,
                TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        String prompt = builder.buildUserPrompt(CATAN, profile);

        assertTrue(prompt.contains("Trade and build"));
    }
}
