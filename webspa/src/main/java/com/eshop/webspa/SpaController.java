package com.eshop.webspa;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SpaController {
    @GetMapping(value = {"/", "/catalog", "/basket", "/orders", "/order", "/campaigns"}, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
                <!doctype html><html><head><title>eShop SPA</title>
                <style>body{font-family:Arial;margin:0}.top{background:#83d01b;color:white;padding:18px 32px}.nav{padding:12px 32px;background:#f6f6f6}.nav a{margin-right:18px}main{padding:32px}.card{border:1px solid #ddd;padding:18px}</style>
                </head><body><div class='top'>eShop SPA</div><div class='nav'><a href='/catalog'>Catalog</a><a href='/basket'>Basket</a><a href='/orders'>Orders</a><a href='/campaigns'>Campaigns</a></div>
                <main><div class='card'><h1>Catalog</h1><p>Angular SPA host placeholder preserving eShop routes and style baseline.</p></div></main></body></html>
                """;
    }

    @GetMapping("/configuration")
    public Map<String, String> config() {
        return Map.of("identityUrl", "http://localhost:5105", "purchaseUrl", "http://localhost:5202", "marketingUrl", "http://localhost:5203", "signalrHubUrl", "http://localhost:5202");
    }
}
