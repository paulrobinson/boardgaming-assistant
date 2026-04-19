package com.boardgaming.assistant.adapter.out.llm;

import com.boardgaming.assistant.application.port.out.TimingEstimateModelPort;
import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import com.boardgaming.assistant.domain.service.TimingHeuristicsService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FakeTimingEstimateModelAdapter implements TimingEstimateModelPort {

    private final TimingHeuristicsService heuristicsService = new TimingHeuristicsService();

    @Override
    public SessionTimingEstimate generate(String estimateId, Game game, GroupProfile profile) {
        return heuristicsService.calculate(estimateId, game, profile);
    }
}
