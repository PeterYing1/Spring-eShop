package com.eshop.ordering;

import com.eshop.common.model.BasketModels.BasketItem;
import com.eshop.common.model.OrderingModels.Address;
import com.eshop.common.model.OrderingModels.CancelOrderCommand;
import com.eshop.common.model.OrderingModels.CardType;
import com.eshop.common.model.OrderingModels.CreateOrderDraftCommand;
import com.eshop.common.model.OrderingModels.Order;
import com.eshop.common.model.OrderingModels.OrderDraftDTO;
import com.eshop.common.model.OrderingModels.OrderItem;
import com.eshop.common.model.OrderingModels.OrderStatus;
import com.eshop.common.model.OrderingModels.OrderSummary;
import com.eshop.common.model.OrderingModels.ShipOrderCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/orders")
public class OrdersController {
    private final AtomicInteger orderIds = new AtomicInteger(1000);
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final List<CardType> cardTypes = List.of(new CardType(1, "Amex"), new CardType(2, "Visa"), new CardType(3, "MasterCard"));

    @PutMapping("/cancel")
    public ResponseEntity<Void> cancel(@RequestBody CancelOrderCommand command,
                                       @RequestHeader(name = "x-requestid", required = false) String requestId) {
        Order order = orders.get(command.orderNumber());
        if (order == null || order.status().equals(OrderStatus.PAID.statusName()) || order.status().equals(OrderStatus.SHIPPED.statusName())) {
            return ResponseEntity.badRequest().build();
        }
        orders.put(command.orderNumber(), withStatus(order, OrderStatus.CANCELLED.statusName()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/ship")
    public ResponseEntity<Void> ship(@RequestBody ShipOrderCommand command,
                                     @RequestHeader(name = "x-requestid", required = false) String requestId) {
        Order order = orders.get(command.orderNumber());
        if (order == null || !order.status().equals(OrderStatus.PAID.statusName())) {
            return ResponseEntity.badRequest().build();
        }
        orders.put(command.orderNumber(), withStatus(order, OrderStatus.SHIPPED.statusName()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable int orderId) {
        Order order = orders.get(orderId);
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }

    @GetMapping
    public List<OrderSummary> getOrders() {
        return orders.values().stream()
                .map(o -> new OrderSummary(o.orderNumber(), o.date(), o.status(), o.total()))
                .toList();
    }

    @GetMapping("/cardtypes")
    public List<CardType> getCardTypes() {
        return cardTypes;
    }

    @PostMapping("/draft")
    public OrderDraftDTO draft(@RequestBody CreateOrderDraftCommand command) {
        List<OrderItem> items = command.items().stream().map(this::toOrderItem).toList();
        BigDecimal total = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.units())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderDraftDTO(total, items);
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody CreateOrderDraftCommand command) {
        OrderDraftDTO draft = draft(command);
        int id = orderIds.incrementAndGet();
        Order order = new Order(id, OffsetDateTime.now(), OrderStatus.SUBMITTED.statusName(), "The order was submitted.",
                new Address("", "", "", "", ""), draft.orderItems(), draft.total());
        orders.put(id, order);
        return ResponseEntity.ok(order);
    }

    private OrderItem toOrderItem(BasketItem item) {
        return new OrderItem(item.productId(), item.productName(), item.unitPrice(), BigDecimal.ZERO, item.quantity(), item.pictureUrl());
    }

    private Order withStatus(Order order, String status) {
        return new Order(order.orderNumber(), order.date(), status, "The order status changed to " + status + ".",
                order.address(), order.orderItems(), order.total());
    }
}
