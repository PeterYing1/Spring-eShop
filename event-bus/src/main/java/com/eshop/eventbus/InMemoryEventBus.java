package com.eshop.eventbus;

import com.eshop.common.model.IntegrationEvents.IntegrationEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventBus implements EventBus {
    private final List<IntegrationEvent> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(IntegrationEvent event) {
        publishedEvents.add(event);
    }

    public List<IntegrationEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
