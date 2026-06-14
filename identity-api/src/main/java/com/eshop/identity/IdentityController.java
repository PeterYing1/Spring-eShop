package com.eshop.identity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class IdentityController {
    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> discovery() {
        return Map.of(
                "issuer", "http://localhost:5105",
                "authorization_endpoint", "http://localhost:5105/oauth2/authorize",
                "token_endpoint", "http://localhost:5105/oauth2/token",
                "jwks_uri", "http://localhost:5105/oauth2/jwks",
                "scopes_supported", List.of("openid", "profile", "orders", "basket", "marketing", "locations", "webshoppingagg", "mobileshoppingagg", "webhooks")
        );
    }

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("service", "identity-api", "status", "started");
    }
}
