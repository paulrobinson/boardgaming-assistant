package com.boardgaming.assistant.adapter.out.analytics;

import com.boardgaming.assistant.application.port.out.EventSinkPort;
import com.boardgaming.assistant.domain.model.Event;
import com.boardgaming.assistant.domain.model.EventType;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class InMemoryEventSinkAdapter implements EventSinkPort {

    private final List<Event> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(Event event) {
        events.add(event);
    }

    public List<Event> allEvents() {
        return List.copyOf(events);
    }

    public List<Event> eventsOfType(EventType type) {
        return events.stream()
                .filter(e -> e.type() == type)
                .toList();
    }
}
