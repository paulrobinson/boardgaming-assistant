package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Game;
import com.boardgaming.assistant.domain.model.GroupProfile;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;

public interface TimingEstimateModelPort {
    SessionTimingEstimate generate(String estimateId, Game game, GroupProfile profile);
}
