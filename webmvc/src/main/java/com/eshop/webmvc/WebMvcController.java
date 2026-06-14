package com.eshop.webmvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.HtmlUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
public class WebMvcController {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String catalogApiBaseUrl;

    public WebMvcController(@Value("${eshop.catalog-api-base-url:http://localhost:5101}") String catalogApiBaseUrl) {
        this.catalogApiBaseUrl = catalogApiBaseUrl;
    }

    @GetMapping(value = {"/", "/Catalog", "/Catalog/Index"}, produces = MediaType.TEXT_HTML_VALUE)
    public String catalog() {
        return page("Catalog", renderCatalog());
    }

    @GetMapping(value = "/Cart", produces = MediaType.TEXT_HTML_VALUE)
    public String cart() {
        return page("My Cart", "<section class='esh-basket'><h2>My Cart</h2><p>Your basket is ready for checkout.</p><button>Checkout</button></section>");
    }

    @GetMapping(value = "/Order", produces = MediaType.TEXT_HTML_VALUE)
    public String order() {
        return page("My Orders", "<section class='esh-orders'><h2>My Orders</h2><p>Order history and details.</p></section>");
    }

    private String page(String title, String body) {
        return """
                <!doctype html><html><head><title>eShop - %s</title>
                <style>
                body{font-family:Arial,sans-serif;margin:0;color:#333}.esh-header{background:#83d01b;color:#fff;padding:18px 32px;font-size:24px}
                .esh-nav{background:#f5f5f5;padding:12px 32px}.esh-nav a{margin-right:18px;color:#333;text-decoration:none}
                main{padding:32px}.esh-catalog{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:24px}
                .esh-catalog-item,.esh-basket,.esh-orders{border:1px solid #ddd;padding:18px;background:#fff}button{background:#83d01b;color:#fff;border:0;padding:10px 18px}
                </style></head><body><header class='esh-header'>eShopOnContainers</header>
                <nav class='esh-nav'><a href='/'>Catalog</a><a href='/Cart'>Basket</a><a href='/Order'>Orders</a></nav><main>%s</main></body></html>
                """.formatted(title, body);
    }

    private String renderCatalog() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(catalogApiBaseUrl + "/api/v1/catalog/items?pageSize=100&pageIndex=0"))
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                return fallbackCatalog();
            }
            JsonNode response = objectMapper.readTree(httpResponse.body());
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                return fallbackCatalog();
            }

            StringBuilder html = new StringBuilder("<section class='esh-catalog'>");
            for (JsonNode item : data) {
                html.append("<article class='esh-catalog-item'>")
                        .append("<h2>").append(escape(item.path("name").asText())).append("</h2>")
                        .append("<p>").append(escape(item.path("description").asText())).append("</p>")
                        .append("<p class='esh-price'>$").append(escape(item.path("price").asText())).append("</p>")
                        .append("<button>Add to basket</button>")
                        .append("</article>");
            }
            html.append("</section>");
            return html.toString();
        } catch (Exception ex) {
            return fallbackCatalog();
        }
    }

    private String fallbackCatalog() {
        return """
                <section class='esh-catalog'>
                  <article class='esh-catalog-item'><h2>.NET Bot Black Hoodie</h2><p>Black hoodie with .NET Bot</p><button>Add to basket</button></article>
                  <article class='esh-catalog-item'><h2>.NET Black & White Mug</h2><p>Ceramic mug</p><button>Add to basket</button></article>
                </section>
                """;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
