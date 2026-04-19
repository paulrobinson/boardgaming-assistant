package com.boardgaming.assistant.adapter.out.analytics;

import com.boardgaming.assistant.application.port.out.AnalyticsPort;
import com.boardgaming.assistant.domain.model.Feedback;
import com.boardgaming.assistant.domain.model.SessionTimingEstimate;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

@ApplicationScoped
public class LoggingAnalyticsAdapter implements AnalyticsPort {

    private static final Logger LOG = Logger.getLogger(LoggingAnalyticsAdapter.class.getName());

    @Override
    public void recordEstimateCreated(SessionTimingEstimate estimate) {
        LOG.info("Estimate created: id=" + estimate.estimateId()
                + " game=" + estimate.gameId()
                + " total=" + estimate.totalMinutes() + "min");
    }

    @Override
    public void recordFeedbackReceived(Feedback feedback) {
        LOG.info("Feedback received: id=" + feedback.feedbackId()
                + " estimate=" + feedback.estimateId()
                + " actualTotal=" + (feedback.actualTeachMinutes() + feedback.actualPlayMinutes()) + "min");
    }
}
