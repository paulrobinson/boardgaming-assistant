package com.boardgaming.assistant.domain.service;

import com.boardgaming.assistant.domain.model.AnalysisStyle;
import com.boardgaming.assistant.domain.model.Confidence;
import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.PlayerCountFit;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;

import java.util.ArrayList;
import java.util.List;

public class TimingHeuristicsService {

    public SessionTimingEstimate calculate(String estimateId, Game game, GroupProfile profile) {
        int teachMinutes = calculateTeachMinutes(game, profile);
        int playMinutes = calculatePlayMinutes(game, profile);
        int totalMinutes = teachMinutes + playMinutes;
        Confidence confidence = determineConfidence(game, profile);
        List<PlayerCountFit> playerCountFit = buildPlayerCountFit(game);
        String explanation = buildExplanation(game, profile, teachMinutes, playMinutes);
        List<String> riskNotes = buildRiskNotes(game, profile);

        return new SessionTimingEstimate(
                estimateId,
                game.gameId(),
                teachMinutes,
                playMinutes,
                totalMinutes,
                confidence,
                playerCountFit,
                explanation,
                riskNotes);
    }

    int calculateTeachMinutes(Game game, GroupProfile profile) {
        double base;
        if (game.complexityWeight() < 2.0) {
            base = 10;
        } else if (game.complexityWeight() < 3.0) {
            base = 20;
        } else if (game.complexityWeight() < 4.0) {
            base = 30;
        } else {
            base = 45;
        }

        double familiarityMultiplier = switch (profile.groupFamiliarity()) {
            case NEW -> 1.5;
            case MIXED -> 1.2;
            case EXPERIENCED -> 0.5;
        };

        return (int) Math.round(base * familiarityMultiplier);
    }

    int calculatePlayMinutes(Game game, GroupProfile profile) {
        double base = game.officialPlayTimeMinutes();

        Fit fit = game.playerCountSummary().getOrDefault(profile.playerCount(), Fit.OK);
        double playerCountMultiplier = switch (fit) {
            case BEST -> 1.0;
            case GOOD -> 1.05;
            case OK -> 1.1;
            case NOT_RECOMMENDED -> 1.2;
            case AVOID -> 1.3;
        };

        double paceMultiplier = switch (profile.turnPace()) {
            case FAST -> 0.85;
            case AVERAGE -> 1.0;
            case SLOW -> 1.25;
        };

        double analysisMultiplier = switch (profile.analysisStyle()) {
            case LOW -> 0.9;
            case MODERATE -> 1.1;
            case HIGH -> 1.3;
        };

        double result = base * playerCountMultiplier * paceMultiplier * analysisMultiplier;

        if (profile.childrenIncluded()) {
            result *= 1.15;
        }

        return (int) Math.round(result);
    }

    Confidence determineConfidence(Game game, GroupProfile profile) {
        int playerCount = profile.playerCount();

        if (playerCount < game.minPlayers() || playerCount > game.maxPlayers()) {
            return Confidence.LOW;
        }

        Fit fit = game.playerCountSummary().getOrDefault(playerCount, Fit.OK);

        if (fit == Fit.AVOID || fit == Fit.NOT_RECOMMENDED) {
            return Confidence.LOW;
        }

        if (profile.groupFamiliarity() == com.boardgaming.assistant.domain.model.GroupFamiliarity.EXPERIENCED
                && (fit == Fit.BEST || fit == Fit.GOOD)) {
            return Confidence.HIGH;
        }

        return Confidence.MEDIUM;
    }

    List<PlayerCountFit> buildPlayerCountFit(Game game) {
        List<PlayerCountFit> result = new ArrayList<>();
        for (int count = game.minPlayers(); count <= game.maxPlayers(); count++) {
            Fit fit = game.playerCountSummary().getOrDefault(count, Fit.OK);
            result.add(new PlayerCountFit(count, fit));
        }
        return result;
    }

    String buildExplanation(Game game, GroupProfile profile, int teachMinutes, int playMinutes) {
        var sb = new StringBuilder();
        sb.append(game.name());

        Fit fit = game.playerCountSummary().getOrDefault(profile.playerCount(), Fit.OK);
        switch (fit) {
            case BEST -> sb.append(" plays best");
            case GOOD -> sb.append(" plays well");
            case OK -> sb.append(" is playable");
            case NOT_RECOMMENDED -> sb.append(" is not ideal");
            case AVOID -> sb.append(" is not recommended");
        }
        sb.append(" at ").append(profile.playerCount()).append(" players. ");

        if (profile.groupFamiliarity() == com.boardgaming.assistant.domain.model.GroupFamiliarity.NEW) {
            sb.append("A full rules teach adds ").append(teachMinutes).append(" minutes. ");
        } else if (profile.groupFamiliarity() == com.boardgaming.assistant.domain.model.GroupFamiliarity.MIXED) {
            sb.append("Mixed familiarity means some rules review, adding ").append(teachMinutes).append(" minutes. ");
        } else {
            sb.append("Experienced players need minimal rules review. ");
        }

        int officialTime = game.officialPlayTimeMinutes();
        if (playMinutes > officialTime) {
            sb.append("Play time is estimated above the official ")
                    .append(officialTime)
                    .append(" minutes due to group pace and style.");
        } else {
            sb.append("Play time is close to the official ")
                    .append(officialTime)
                    .append(" minutes.");
        }

        return sb.toString();
    }

    List<String> buildRiskNotes(Game game, GroupProfile profile) {
        List<String> notes = new ArrayList<>();

        if (profile.groupFamiliarity() == com.boardgaming.assistant.domain.model.GroupFamiliarity.NEW
                || profile.groupFamiliarity() == com.boardgaming.assistant.domain.model.GroupFamiliarity.MIXED) {
            notes.add("Rules reminders may increase downtime");
        }

        if (profile.groupFamiliarity() == com.boardgaming.assistant.domain.model.GroupFamiliarity.NEW) {
            notes.add("First play may run longer than the estimate");
        }

        if (profile.turnPace() == com.boardgaming.assistant.domain.model.TurnPace.SLOW) {
            notes.add("Slow turn pace can compound with higher player counts");
        }

        if (profile.analysisStyle() == AnalysisStyle.HIGH) {
            notes.add("Analysis-heavy players may extend decision phases significantly");
        }

        if (profile.childrenIncluded()) {
            notes.add("Sessions with children often include extra explanation and downtime");
        }

        Fit fit = game.playerCountSummary().getOrDefault(profile.playerCount(), Fit.OK);
        if (fit == Fit.NOT_RECOMMENDED || fit == Fit.AVOID) {
            notes.add("The game may not play well at this player count");
        }

        return notes;
    }
}
