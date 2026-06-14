package com.eshop.common.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class OrderingModels {
    private OrderingModels() {
    }

    public enum OrderStatus {
        SUBMITTED(1, "submitted"),
        AWAITING_VALIDATION(2, "awaitingvalidation"),
        STOCK_CONFIRMED(3, "stockconfirmed"),
        PAID(4, "paid"),
        SHIPPED(5, "shipped"),
        CANCELLED(6, "cancelled");

        private final int id;
        private final String name;

        OrderStatus(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int id() {
            return id;
        }

        public String statusName() {
            return name;
        }
    }

    public record Address(String street, String city, String state, String country, String zipCode) {
    }

    public record OrderItem(Integer productId, String productName, BigDecimal unitPrice, BigDecimal discount,
                            Integer units, String pictureUrl) {
    }

    public record Order(Integer orderNumber, OffsetDateTime date, String status, String description,
                        Address address, List<OrderItem> orderItems, BigDecimal total) {
    }

    public record OrderSummary(Integer orderNumber, OffsetDateTime date, String status, BigDecimal total) {
    }

    public record CardType(Integer id, String name) {
    }

    public record CancelOrderCommand(Integer orderNumber) {
    }

    public record ShipOrderCommand(Integer orderNumber) {
    }

    public record CreateOrderDraftCommand(String buyerId, List<BasketModels.BasketItem> items) {
    }

    public record OrderDraftDTO(BigDecimal total, List<OrderItem> orderItems) {
    }
}
