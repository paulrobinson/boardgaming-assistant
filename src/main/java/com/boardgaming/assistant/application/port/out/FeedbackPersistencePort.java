package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Feedback;

public interface FeedbackPersistencePort {
    void save(Feedback feedback);
}
