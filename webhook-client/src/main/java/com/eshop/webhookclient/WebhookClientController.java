package com.eshop.webhookclient;

import com.eshop.common.model.WebhookModels.WebhookData;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class WebhookClientController {
    private final List<WebhookData> received = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!doctype html><html><head><title>eShop Webhooks</title><style>body{font-family:Arial;margin:32px}.panel{border:1px solid #ddd;padding:18px}</style></head>
                <body><h1>Webhook Client</h1><div class='panel'><p>Register and inspect webhook callbacks.</p><p>Received hooks: %d</p></div></body></html>
                """.formatted(received.size());
    }

    @PostMapping("/webhook-received")
    public void receive(@RequestBody WebhookData data) {
        received.add(data);
    }
}
