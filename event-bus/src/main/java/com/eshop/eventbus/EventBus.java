package com.eshop.eventbus;

import com.eshop.common.model.IntegrationEvents.IntegrationEvent;

public interface EventBus {
    void publish(IntegrationEvent event);
}
