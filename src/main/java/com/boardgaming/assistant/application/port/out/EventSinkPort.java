package com.boardgaming.assistant.application.port.out;

import com.boardgaming.assistant.domain.model.Event;

public interface EventSinkPort {
    void publish(Event event);
}
