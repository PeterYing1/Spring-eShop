package com.eshop.payment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PaymentController {
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("service", "payment-api", "status", "started");
    }
}
