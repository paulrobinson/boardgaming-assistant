package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.application.port.out.TimingEstimateModelPort;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.service.TimingHeuristicsService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LlmTimingEstimateModelAdapter implements TimingEstimateModelPort {

    private static final Logger LOG = Logger.getLogger(LlmTimingEstimateModelAdapter.class.getName());

    private final LlmClient llmClient;
    private final TimingEstimatePromptBuilder promptBuilder;
    private final TimingEstimateResponseParser responseParser;
    private final TimingHeuristicsService fallbackHeuristics;

    public LlmTimingEstimateModelAdapter(LlmClient llmClient,
                                         TimingHeuristicsService fallbackHeuristics) {
        this.llmClient = llmClient;
        this.promptBuilder = new TimingEstimatePromptBuilder();
        this.responseParser = new TimingEstimateResponseParser();
        this.fallbackHeuristics = fallbackHeuristics;
    }

    @Override
    public SessionTimingEstimate generate(String estimateId, Game game, GroupProfile profile) {
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(game, profile);

            String rawResponse = llmClient.complete(systemPrompt, userPrompt);

            return responseParser.parse(estimateId, game.gameId(), rawResponse)
                    .orElseGet(() -> {
                        LOG.warning("LLM response failed validation, falling back to heuristics for game: "
                                + game.gameId());
                        return fallbackHeuristics.calculate(estimateId, game, profile);
                    });

        } catch (Exception e) {
            LOG.log(Level.WARNING, "LLM call failed, falling back to heuristics for game: "
                    + game.gameId(), e);
            return fallbackHeuristics.calculate(estimateId, game, profile);
        }
    }
}
