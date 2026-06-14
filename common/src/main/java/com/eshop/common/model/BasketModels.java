package com.eshop.common.model;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class BasketModels {
    private BasketModels() {
    }

    public record CustomerBasket(String buyerId, List<BasketItem> items) {
        public CustomerBasket {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record BasketItem(
            String id,
            Integer productId,
            String productName,
            BigDecimal unitPrice,
            BigDecimal oldUnitPrice,
            @Min(1) Integer quantity,
            String pictureUrl) {
    }

    public record BasketCheckout(
            String city,
            String street,
            String state,
            String country,
            String zipCode,
            String cardNumber,
            String cardHolderName,
            OffsetDateTime cardExpiration,
            String cardSecurityNumber,
            Integer cardTypeId,
            String buyer,
            UUID requestId) {
    }
}
