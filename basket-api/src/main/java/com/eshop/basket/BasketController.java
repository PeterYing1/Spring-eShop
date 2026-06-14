package com.eshop.basket;

import com.eshop.common.model.BasketModels.BasketCheckout;
import com.eshop.common.model.BasketModels.CustomerBasket;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/basket")
public class BasketController {
    private final Map<String, CustomerBasket> baskets = new ConcurrentHashMap<>();

    @GetMapping("/{id}")
    public CustomerBasket getBasket(@PathVariable String id) {
        return baskets.getOrDefault(id, new CustomerBasket(id, List.of()));
    }

    @PostMapping
    public CustomerBasket updateBasket(@Valid @RequestBody CustomerBasket basket) {
        baskets.put(basket.buyerId(), basket);
        return basket;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkout(@RequestBody BasketCheckout checkout,
                                         @RequestHeader(name = "x-requestid", required = false) String requestId) {
        String buyer = checkout.buyer() == null || checkout.buyer().isBlank() ? "anonymous" : checkout.buyer();
        if (!baskets.containsKey(buyer)) {
            return ResponseEntity.badRequest().build();
        }
        UUID ignored = parseRequestId(requestId, checkout.requestId());
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBasket(@PathVariable String id) {
        baskets.remove(id);
        return ResponseEntity.ok().build();
    }

    private UUID parseRequestId(String header, UUID fallback) {
        try {
            return header == null || header.isBlank() ? fallback : UUID.fromString(header);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
