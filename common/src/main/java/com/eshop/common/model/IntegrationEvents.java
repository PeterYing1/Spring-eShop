package com.eshop.common.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class IntegrationEvents {
    private IntegrationEvents() {
    }

    public interface IntegrationEvent {
        UUID id();

        OffsetDateTime creationDate();
    }

    public record ProductPriceChangedIntegrationEvent(UUID id, OffsetDateTime creationDate, Integer productId,
                                                      BigDecimal newPrice, BigDecimal oldPrice)
            implements IntegrationEvent {
    }

    public record UserCheckoutAcceptedIntegrationEvent(UUID id, OffsetDateTime creationDate, String userId,
                                                       String userName, BasketModels.BasketCheckout checkout,
                                                       BasketModels.CustomerBasket basket)
            implements IntegrationEvent {
    }

    public record OrderStockItem(Integer productId, Integer units) {
    }

    public record OrderStatusChangedToAwaitingValidationIntegrationEvent(UUID id, OffsetDateTime creationDate,
                                                                        Integer orderId,
                                                                        List<OrderStockItem> orderStockItems)
            implements IntegrationEvent {
    }

    public record OrderStatusChangedIntegrationEvent(UUID id, OffsetDateTime creationDate, Integer orderId,
                                                    String status)
            implements IntegrationEvent {
    }

    public record UserLocationUpdatedIntegrationEvent(UUID id, OffsetDateTime creationDate, String userId,
                                                     List<LocationModels.UserLocationDetails> locationList)
            implements IntegrationEvent {
    }
}
