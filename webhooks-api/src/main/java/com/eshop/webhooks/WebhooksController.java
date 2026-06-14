package com.eshop.webhooks;

import com.eshop.common.model.WebhookModels.WebhookSubscription;
import com.eshop.common.model.WebhookModels.WebhookSubscriptionRequest;
import com.eshop.common.model.WebhookModels.WebhookType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhooksController {
    private final AtomicInteger ids = new AtomicInteger();
    private final Map<Integer, WebhookSubscription> subscriptions = new ConcurrentHashMap<>();

    @GetMapping
    public List<WebhookSubscription> listByUser() {
        return subscriptions.values().stream().toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookSubscription> get(@PathVariable int id) {
        WebhookSubscription subscription = subscriptions.get(id);
        return subscription == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(subscription);
    }

    @PostMapping
    public ResponseEntity<?> subscribe(@RequestBody WebhookSubscriptionRequest request) {
        if (request.url() == null || request.url().isBlank() || request.event() == null || request.event().isBlank()) {
            return ResponseEntity.badRequest().body("Invalid webhook subscription request");
        }
        int id = ids.incrementAndGet();
        WebhookSubscription subscription = new WebhookSubscription(id, OffsetDateTime.now(), request.url(),
                request.token(), WebhookType.valueOf(request.event()), "anonymous");
        subscriptions.put(id, subscription);
        return ResponseEntity.created(URI.create("/api/v1/webhooks/" + id)).body(subscription);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> unsubscribe(@PathVariable int id) {
        return subscriptions.remove(id) == null
                ? ResponseEntity.status(404).body("Subscriptions " + id + " not found")
                : ResponseEntity.accepted().build();
    }
}
