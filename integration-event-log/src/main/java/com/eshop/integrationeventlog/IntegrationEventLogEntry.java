package com.eshop.integrationeventlog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationEventLogEntry(UUID eventId, String eventTypeName, EventState state, int timesSent,
                                       OffsetDateTime creationTime, String content, UUID transactionId) {
}
