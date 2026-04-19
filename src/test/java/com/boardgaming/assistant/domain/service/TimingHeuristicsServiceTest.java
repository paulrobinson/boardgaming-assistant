package com.boardgaming.assistant.domain.service;

import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupFamiliarity;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.model.TurnPace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingHeuristicsServiceTest {

    private TimingHeuristicsService service;

    private static final Game CATAN = new Game(
            "catan", "0029877030712", "Catan", 3, 4, 60, 10, 2.3,
            Map.of(3, Fit.GOOD, 4, Fit.BEST),
            "Trading and building");

    private static final Game SIMPLE_GAME = new Game(
            "simple", "0000000000001", "Simple Game", 2, 6, 30, 6, 1.5,
            Map.of(2, Fit.GOOD, 3, Fit.BEST, 4, Fit.GOOD, 5, Fit.OK, 6, Fit.NOT_RECOMMENDED),
            "A simple game");

    private static final Game COMPLEX_GAME = new Game(
            "complex", "0000000000002", "Complex Game", 1, 4, 120, 14, 4.2,
            Map.of(1, Fit.AVOID, 2, Fit.OK, 3, Fit.BEST, 4, Fit.GOOD),
            "A complex game");

    @BeforeEach
    void setUp() {
        service = new TimingHeuristicsService();
    }

    @Test
    void totalMinutesEqualsSumOfTeachAndPlay() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        SessionTimingEstimate result = service.calculate("est_001", CATAN, profile);

        assertEquals(result.teachMinutes() + result.playMinutes(), result.totalMinutes());
    }

    @Test
    void newGroupGetsLongerTeachTime() {
        GroupProfile newGroup = new GroupProfile(4, GroupFamiliarity.NEW, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        GroupProfile experienced = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        int teachNew = service.calculateTeachMinutes(CATAN, newGroup);
        int teachExp = service.calculateTeachMinutes(CATAN, experienced);

        assertTrue(teachNew > teachExp, "New group should need more teach time than experienced group");
    }

    @Test
    void mixedGroupTeachTimeBetweenNewAndExperienced() {
        GroupProfile newGroup = new GroupProfile(4, GroupFamiliarity.NEW, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        GroupProfile mixed = new GroupProfile(4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        GroupProfile experienced = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        int teachNew = service.calculateTeachMinutes(CATAN, newGroup);
        int teachMixed = service.calculateTeachMinutes(CATAN, mixed);
        int teachExp = service.calculateTeachMinutes(CATAN, experienced);

        assertTrue(teachMixed < teachNew, "Mixed should be less than new");
        assertTrue(teachMixed > teachExp, "Mixed should be more than experienced");
    }

    @Test
    void slowPaceIncreasesPlayTime() {
        GroupProfile fast = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.FAST, AnalysisStyle.MODERATE, false, null);
        GroupProfile slow = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.SLOW, AnalysisStyle.MODERATE, false, null);

        int playFast = service.calculatePlayMinutes(CATAN, fast);
        int playSlow = service.calculatePlayMinutes(CATAN, slow);

        assertTrue(playSlow > playFast, "Slow pace should produce longer play time");
    }

    @Test
    void highAnalysisIncreasesPlayTime() {
        GroupProfile low = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.LOW, false, null);
        GroupProfile high = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.HIGH, false, null);

        int playLow = service.calculatePlayMinutes(CATAN, low);
        int playHigh = service.calculatePlayMinutes(CATAN, high);

        assertTrue(playHigh > playLow, "High analysis style should produce longer play time");
    }

    @Test
    void childrenIncludedIncreasesPlayTime() {
        GroupProfile noChildren = new GroupProfile(4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        GroupProfile withChildren = new GroupProfile(4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, true, null);

        int playNoKids = service.calculatePlayMinutes(CATAN, noChildren);
        int playWithKids = service.calculatePlayMinutes(CATAN, withChildren);

        assertTrue(playWithKids > playNoKids, "Including children should increase play time");
    }

    @Test
    void highConfidenceForExperiencedGroupAtBestPlayerCount() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.LOW, false, null);
        SessionTimingEstimate result = service.calculate("est_002", CATAN, profile);

        assertEquals(Confidence.HIGH, result.confidence());
    }

    @Test
    void lowConfidenceForPlayerCountOutOfRange() {
        GroupProfile profile = new GroupProfile(6, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.LOW, false, null);
        SessionTimingEstimate result = service.calculate("est_003", CATAN, profile);

        assertEquals(Confidence.LOW, result.confidence());
    }

    @Test
    void lowConfidenceWhenPlayerCountFitIsAvoid() {
        GroupProfile profile = new GroupProfile(1, GroupFamiliarity.EXPERIENCED, TurnPace.AVERAGE, AnalysisStyle.LOW, false, null);
        SessionTimingEstimate result = service.calculate("est_004", COMPLEX_GAME, profile);

        assertEquals(Confidence.LOW, result.confidence());
    }

    @Test
    void playerCountFitListCoversAllSupportedCounts() {
        GroupProfile profile = new GroupProfile(3, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        SessionTimingEstimate result = service.calculate("est_005", SIMPLE_GAME, profile);

        assertEquals(5, result.playerCountFit().size());
        assertEquals(2, result.playerCountFit().get(0).playerCount());
        assertEquals(6, result.playerCountFit().get(4).playerCount());
    }

    @Test
    void complexGameGetsHigherTeachTime() {
        GroupProfile profile = new GroupProfile(3, GroupFamiliarity.NEW, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);

        int teachSimple = service.calculateTeachMinutes(SIMPLE_GAME, profile);
        int teachComplex = service.calculateTeachMinutes(COMPLEX_GAME, profile);

        assertTrue(teachComplex > teachSimple, "Complex game should need more teach time");
    }

    @Test
    void estimateContainsExplanation() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED, TurnPace.SLOW, AnalysisStyle.MODERATE, false, null);
        SessionTimingEstimate result = service.calculate("est_006", CATAN, profile);

        assertNotNull(result.explanation());
        assertFalse(result.explanation().isBlank());
        assertTrue(result.explanation().contains("Catan"));
    }

    @Test
    void riskNotesIncludedForNewGroup() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.NEW, TurnPace.SLOW, AnalysisStyle.HIGH, false, null);
        SessionTimingEstimate result = service.calculate("est_007", CATAN, profile);

        assertFalse(result.riskNotes().isEmpty());
        assertTrue(result.riskNotes().stream().anyMatch(n -> n.toLowerCase().contains("first play")));
    }

    @Test
    void riskNotesIncludeChildrenWarning() {
        GroupProfile profile = new GroupProfile(4, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, true, null);
        SessionTimingEstimate result = service.calculate("est_008", CATAN, profile);

        assertTrue(result.riskNotes().stream().anyMatch(n -> n.toLowerCase().contains("children")));
    }

    @Test
    void estimateIdPreserved() {
        GroupProfile profile = new GroupProfile(3, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        SessionTimingEstimate result = service.calculate("my-custom-id", CATAN, profile);

        assertEquals("my-custom-id", result.estimateId());
    }

    @Test
    void gameIdPreserved() {
        GroupProfile profile = new GroupProfile(3, GroupFamiliarity.MIXED, TurnPace.AVERAGE, AnalysisStyle.MODERATE, false, null);
        SessionTimingEstimate result = service.calculate("est_009", CATAN, profile);

        assertEquals("catan", result.gameId());
    }
}
