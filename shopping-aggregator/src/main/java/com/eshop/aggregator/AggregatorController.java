package com.eshop.aggregator;

import com.eshop.common.model.BasketModels.BasketItem;
import com.eshop.common.model.BasketModels.CustomerBasket;
import com.eshop.common.model.OrderingModels.CreateOrderDraftCommand;
import com.eshop.common.model.OrderingModels.OrderDraftDTO;
import com.eshop.common.model.OrderingModels.OrderItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
public class AggregatorController {
    private final Map<String, CustomerBasket> baskets = new ConcurrentHashMap<>();

    @PostMapping("/basket")
    @PutMapping("/basket")
    public CustomerBasket updateAll(@RequestBody CustomerBasket basket) {
        baskets.put(basket.buyerId(), basket);
        return basket;
    }

    @PutMapping("/basket/items")
    public CustomerBasket updateItems(@RequestBody CustomerBasket basket) {
        baskets.put(basket.buyerId(), basket);
        return basket;
    }

    @PostMapping("/basket/items")
    public void addItem(@RequestBody CustomerBasket basket) {
        baskets.put(basket.buyerId(), basket);
    }

    @GetMapping("/order/draft/{basketId}")
    public OrderDraftDTO draft(@PathVariable String basketId) {
        CustomerBasket basket = baskets.getOrDefault(basketId, new CustomerBasket(basketId, List.of()));
        List<OrderItem> items = basket.items().stream().map(this::toOrderItem).toList();
        BigDecimal total = items.stream().map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.units()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderDraftDTO(total, items);
    }

    @PostMapping("/orders/draft")
    public OrderDraftDTO draftFromCommand(@RequestBody CreateOrderDraftCommand command) {
        List<OrderItem> items = command.items().stream().map(this::toOrderItem).toList();
        BigDecimal total = items.stream().map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.units()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderDraftDTO(total, items);
    }

    private OrderItem toOrderItem(BasketItem item) {
        return new OrderItem(item.productId(), item.productName(), item.unitPrice(), BigDecimal.ZERO, item.quantity(), item.pictureUrl());
    }
}
