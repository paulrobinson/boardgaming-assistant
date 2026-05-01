package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.domain.model.Fit;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupProfile;

import java.util.Map;
import java.util.StringJoiner;

public class TimingEstimatePromptBuilder {

    public String buildSystemPrompt() {
        return """
                You are a board-game session timing estimator.
                Your task is to estimate how long a board-game session will take in minutes, \
                given game metadata and a group profile.

                Respond ONLY with a valid JSON object in the following format:
                {
                  "teachMinutes": <int>,
                  "playMinutes": <int>,
                  "totalMinutes": <int>,
                  "confidence": "<low|medium|high>",
                  "playerCountFit": [
                    {"playerCount": <int>, "fit": "<best|good|ok|not_recommended|avoid>"}
                  ],
                  "explanation": "<string>",
                  "riskNotes": ["<string>"]
                }

                Rules:
                - "estimate" always means a time estimate in minutes, never a price or cost.
                - totalMinutes MUST equal teachMinutes + playMinutes.
                - teachMinutes and playMinutes must each be greater than zero.
                - confidence must be one of: low, medium, high.
                - playerCountFit must cover every supported player count from minPlayers to maxPlayers.
                - fit must be one of: best, good, ok, not_recommended, avoid.
                - Do not include any text outside the JSON object.""";
    }

    public String buildUserPrompt(Game game, GroupProfile profile) {
        var sb = new StringBuilder();
        sb.append("Estimate the session time for the following:\n\n");

        sb.append("Game:\n");
        sb.append("  Name: ").append(game.name()).append("\n");
        sb.append("  Official play time: ").append(game.officialPlayTimeMinutes()).append(" minutes\n");
        sb.append("  Players: ").append(game.minPlayers()).append("-").append(game.maxPlayers()).append("\n");
        sb.append("  Complexity weight: ").append(game.complexityWeight()).append("\n");
        sb.append("  Minimum age: ").append(game.officialMinAge()).append("\n");

        if (game.playerCountSummary() != null && !game.playerCountSummary().isEmpty()) {
            sb.append("  Player count fit: ");
            var joiner = new StringJoiner(", ");
            for (Map.Entry<Integer, Fit> entry : game.playerCountSummary().entrySet()) {
                joiner.add(entry.getKey() + "=" + entry.getValue().name().toLowerCase());
            }
            sb.append(joiner).append("\n");
        }

        if (game.notes() != null && !game.notes().isBlank()) {
            sb.append("  Notes: ").append(game.notes()).append("\n");
        }

        sb.append("\nGroup:\n");
        sb.append("  Player count: ").append(profile.playerCount()).append("\n");
        sb.append("  Familiarity: ").append(profile.groupFamiliarity().name().toLowerCase()).append("\n");
        sb.append("  Turn pace: ").append(profile.turnPace().name().toLowerCase()).append("\n");
        sb.append("  Analysis style: ").append(profile.analysisStyle().name().toLowerCase()).append("\n");
        sb.append("  Children included: ").append(profile.childrenIncluded()).append("\n");

        if (profile.notes() != null && !profile.notes().isBlank()) {
            sb.append("  Notes: ").append(profile.notes()).append("\n");
        }

        return sb.toString();
    }
}
