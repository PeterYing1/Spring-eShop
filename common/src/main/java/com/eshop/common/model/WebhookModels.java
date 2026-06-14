package com.eshop.common.model;

import java.time.OffsetDateTime;

public final class WebhookModels {
    private WebhookModels() {
    }

    public enum WebhookType {
        ProductPriceChanged,
        OrderPaid,
        OrderShipped
    }

    public record WebhookSubscription(Integer id, OffsetDateTime date, String destUrl, String token,
                                      WebhookType type, String userId) {
    }

    public record WebhookSubscriptionRequest(String url, String grantUrl, String token, String event) {
    }

    public record WebhookData(String type, Object data) {
    }
}
