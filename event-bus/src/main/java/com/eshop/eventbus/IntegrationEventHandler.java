package com.eshop.eventbus;

import com.eshop.common.model.IntegrationEvents.IntegrationEvent;

@FunctionalInterface
public interface IntegrationEventHandler<T extends IntegrationEvent> {
    void handle(T event);
}
