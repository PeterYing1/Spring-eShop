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
        return shopPage("Catalog", "<a class='esh-primary-button' href='/Payment'>Checkout</a>", renderCatalog());
    }

    @GetMapping(value = "/Cart", produces = MediaType.TEXT_HTML_VALUE)
    public String cart() {
        return payment();
    }

    @GetMapping(value = "/Payment", produces = MediaType.TEXT_HTML_VALUE)
    public String payment() {
        return shopPage("Payment", "", paymentSummaryPage());
    }

    @GetMapping(value = "/Checkout", produces = MediaType.TEXT_HTML_VALUE)
    public String checkout() {
        return creditCardPaymentPage();
    }

    @GetMapping(value = "/Order", produces = MediaType.TEXT_HTML_VALUE)
    public String order() {
        return page("My Orders", "<div class='container'><section class='esh-orders'><h2>My Orders</h2><p>Order history and details.</p></section></div>");
    }

    private String page(String title, String body) {
        return shopPage(title, "", body);
    }

    private String shopPage(String title, String headerAction, String body) {
        return """
                <!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>Spring eShop MVP</title>
                <link rel='stylesheet' href='/css/app.css'>
                <link rel='stylesheet' href='/css/spring-eshop.css'>
                <script src='/js/catalog-basket.js' defer></script>
                </head><body class='esh-shop-body'>
                <header class='esh-shop-header'>
                  <div>
                    <p>Spring Boot MVP</p>
                    <a href='/' class='esh-shop-brand'>eShopOnContainers</a>
                  </div>
                  <div class='esh-shop-header-action'>%s</div>
                </header>
                %s
                </body></html>
                """.formatted(headerAction, body);
    }

    private String paymentSummaryPage() {
        return """
                <main class='esh-shop-main'>
                  <div class='esh-page-title-row'>
                    <h1>Payment</h1>
                    <a class='esh-secondary-button' href='/'>Back to Shop</a>
                  </div>
                  <section class='esh-order-card'>
                    <h2 data-order-number>Order #1</h2>
                    <p class='esh-muted' data-order-submitted>Submitted</p>
                    <div class='esh-card-divider'></div>
                    <ul class='esh-order-lines' data-order-items></ul>
                    <div class='esh-card-divider'></div>
                    <div class='esh-order-total-row'>
                      <span>Total</span>
                      <strong data-payment-total>$0.00</strong>
                    </div>
                  </section>
                  <a class='esh-primary-button esh-pay-order' href='/Checkout'>Pay Order</a>
                </main>
                """;
    }

    private String creditCardPaymentPage() {
        return """
                <!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>Spring eShop MVP</title>
                <link rel='stylesheet' href='/css/app.css'>
                <link rel='stylesheet' href='/css/spring-eshop.css'>
                <script src='/js/catalog-basket.js' defer></script>
                </head><body class='esh-shop-body esh-payment-body'>
                <header class='esh-shop-header'>
                  <div>
                    <p>Spring Boot MVP</p>
                    <a href='/' class='esh-shop-brand'>eShopOnContainers</a>
                  </div>
                  <div class='esh-payment-step esh-shop-header-action'>
                    <span>Enter card details</span>
                  </div>
                </header>
                <main class='esh-payment-main'>
                  <div class='esh-payment-title-row'>
                    <h1>Credit Card Payment</h1>
                    <a class='esh-payment-back' href='/Payment'>Back to Payment</a>
                  </div>
                  <section class='esh-payment-grid'>
                    <aside class='esh-payment-summary'>
                      <h2 data-order-number>Order #1</h2>
                      <p>Amount due</p>
                      <div class='esh-payment-divider'></div>
                      <div class='esh-payment-total-row'>
                        <span>Total</span>
                        <strong data-payment-total>$0.00</strong>
                      </div>
                    </aside>
                    <form class='esh-payment-card'>
                      <label>Cardholder Name
                        <input type='text' value='Demo User'>
                      </label>
                      <label>Card Type
                        <select>
                          <option>Visa</option>
                          <option>Mastercard</option>
                          <option>American Express</option>
                        </select>
                      </label>
                      <label>Card Number
                        <input type='text' value='4012888888881881'>
                      </label>
                      <div class='esh-payment-form-row'>
                        <label>Expiration
                          <input type='text' value='12/28'>
                        </label>
                        <label>Security Code
                          <input type='text' value='123'>
                        </label>
                      </div>
                      <a class='esh-payment-submit' href='/' data-submit-payment>Submit Payment</a>
                    </form>
                  </section>
                </main>
                </body></html>
                """;
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

            StringBuilder html = new StringBuilder("""
                    <main class='esh-shop-main'>
                      <div class='esh-catalog-title-row'>
                        <h1>Catalog</h1>
                        <span>%d items</span>
                      </div>
                      <section class='esh-catalog-layout'>
                        <div class='esh-product-grid'>
                    """.formatted(data.size()));
            for (JsonNode item : data) {
                html.append(renderCatalogItem(item));
            }
            html.append("</div>").append(renderBasketAside()).append("</section></main>");
            return html.toString();
        } catch (Exception ex) {
            return fallbackCatalog();
        }
    }

    private String fallbackCatalog() {
        return """
                <main class='esh-shop-main'>
                  <div class='esh-catalog-title-row'>
                    <h1>Catalog</h1>
                    <span>2 items</span>
                  </div>
                  <section class='esh-catalog-layout'>
                    <div class='esh-product-grid'>
                      <article class='esh-product-card'>
                        <div class='esh-product-art'><strong>eShop</strong><span>.NET Bot Black Hoodie</span></div>
                        <div class='esh-product-body'>
                          <h2>.NET Bot Black Hoodie</h2>
                          <p>.NET - T-Shirt - 100 in stock</p>
                          <strong class='esh-product-price'>$19.50</strong>
                          <button class='esh-product-button' type='button' data-add-to-basket data-product-id='fallback-hoodie' data-product-name='.NET Bot Black Hoodie' data-product-price='19.50'>Add to basket</button>
                        </div>
                      </article>
                      <article class='esh-product-card'>
                        <div class='esh-product-art'><strong>eShop</strong><span>.NET Black & White Mug</span></div>
                        <div class='esh-product-body'>
                          <h2>.NET Black & White Mug</h2>
                          <p>.NET - Mug - 89 in stock</p>
                          <strong class='esh-product-price'>$8.50</strong>
                          <button class='esh-product-button' type='button' data-add-to-basket data-product-id='fallback-mug' data-product-name='.NET Black & White Mug' data-product-price='8.50'>Add to basket</button>
                        </div>
                      </article>
                    </div>
                  %s
                  </section>
                </main>
                """.formatted(renderBasketAside());
    }

    private String renderCatalogItem(JsonNode item) {
        String id = escape(item.path("id").asText(item.path("name").asText()));
        String name = escape(item.path("name").asText());
        String price = escape(item.path("price").asText());
        String brand = escape(item.path("catalogBrand").path("brand").asText(".NET"));
        String type = escape(item.path("catalogType").path("type").asText("Item"));
        String stock = escape(item.path("availableStock").asText("0"));
        return """
                <article class='esh-product-card'>
                  <div class='esh-product-art'><strong>eShop</strong><span>%s</span></div>
                  <div class='esh-product-body'>
                    <h2>%s</h2>
                    <p>%s - %s - %s in stock</p>
                    <strong class='esh-product-price'>$%s</strong>
                    <button class='esh-product-button' type='button' data-add-to-basket data-product-id='%s' data-product-name='%s' data-product-price='%s'>Add to basket</button>
                  </div>
                </article>
                """.formatted(
                name,
                name,
                brand,
                type,
                stock,
                price,
                id,
                name,
                price);
    }

    private String renderBasketAside() {
        return """
                <aside class='esh-basket-card' aria-label='Basket' data-basket>
                  <div class='esh-basket-card-header'>
                    <h2>Basket</h2>
                    <strong data-basket-total>$0.00</strong>
                  </div>
                  <p data-basket-empty>Your basket is empty.</p>
                  <ul class='esh-basket-items' data-basket-items aria-live='polite'></ul>
                  <section class='esh-orders-panel'>
                    <h2>Orders</h2>
                    <p data-orders-empty>No orders yet.</p>
                    <ul class='esh-orders-list' data-orders-list></ul>
                  </section>
                </aside>
                """;
    }

    private String renderBasket(String element, String title, String actionHref, String actionText) {
        return """
                <%s class='esh-basket' aria-label='%s' data-basket>
                  <h2>%s <span data-basket-count>(0)</span></h2>
                  <p data-basket-empty>Your basket is empty.</p>
                  <ul class='esh-basket-items' data-basket-items aria-live='polite'></ul>
                  <p class='esh-basket-total'>Total: <span data-basket-total>$0.00</span></p>
                  <a class='esh-button' href='%s'>%s</a>
                </%s>
                """.formatted(element, title, title, actionHref, actionText, element);
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
