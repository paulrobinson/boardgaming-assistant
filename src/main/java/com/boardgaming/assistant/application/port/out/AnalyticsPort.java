package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;

public interface AnalyticsPort {
    void recordEstimateCreated(SessionTimingEstimate estimate);

    void recordFeedbackReceived(Feedback feedback);
}
