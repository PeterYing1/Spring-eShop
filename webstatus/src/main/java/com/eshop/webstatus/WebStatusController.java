package com.eshop.webstatus;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WebStatusController {
    @GetMapping(value = {"/", "/hc-ui"}, produces = MediaType.TEXT_HTML_VALUE)
    public String status() {
        return """
                <!doctype html><html><head><title>eShop Status</title><style>body{font-family:Arial;margin:32px}.ok{color:#4b9f18}.row{padding:10px;border-bottom:1px solid #ddd}</style></head>
                <body><h1>eShop Health Checks</h1><div class='row'>Catalog API <strong class='ok'>ready</strong></div><div class='row'>Basket API <strong class='ok'>ready</strong></div><div class='row'>Ordering API <strong class='ok'>ready</strong></div></body></html>
                """;
    }

    @GetMapping("/Config")
    public Map<String, String> config() {
        return Map.of("Catalog API", "http://catalog-api/hc", "Basket API", "http://basket-api/hc", "Ordering API", "http://ordering-api/hc");
    }
}
