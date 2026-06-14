# Spring Boot Conversion Design for eShopOnContainers

## 1. Purpose and Baseline

This document defines the implementation design for converting the sibling .NET eShopOnContainers application into a Java Spring Boot application while preserving the same features, public API, deployment topology, and microservice architecture.

Source baseline:

- Source repository: `C:\Projects\eShopOnContainers`
- Source branch: `main` / `origin/main`
- Source commit: `31ab9b62b9fb02fb1c1eb7cadef285c5e6ca6731`
- Target repository: `C:\Projects\Spring-eShop`

Scope:

- Convert the complete server-side application stack to Spring Boot.
- Preserve backend services, BFF aggregators, identity service, background workers, ordering notifications, health/status app, MVC web app, webhook client, and SPA hosting/client behavior.
- Preserve public REST APIs, gRPC contracts, gateway routes, OIDC/OAuth behavior, event contracts, ports, health checks, and local Docker Compose workflows.
- Keep Envoy as the gateway layer.

Out of scope:

- Microsoft SignalR wire-protocol compatibility. The converted UI will use Spring WebSocket/STOMP at the same exposed hub path, `/hub/notificationhub`.
- Rewriting the mobile Xamarin client. Its API and identity contracts must remain compatible.
- Changing product features, event names, database ownership boundaries, or external URLs for cleanup only.

Target technology baseline:

- Java 21.
- Maven multi-module build.
- Spring Boot 4.x, current official Spring Boot line as checked during planning.
- Spring Web MVC, Spring Security, Spring Authorization Server, Spring Data JPA, Spring Data MongoDB, Spring Data Redis, Spring AMQP, Spring gRPC, Spring WebSocket/STOMP, Thymeleaf, Actuator, Micrometer, OpenAPI, Flyway or Liquibase, and Testcontainers.

## 2. Architecture Overview

The Java application must preserve the current eShopOnContainers architecture:

- Autonomous microservices own their data.
- Synchronous client-to-service communication uses HTTP/REST and gRPC.
- Cross-service state propagation uses RabbitMQ integration events.
- The shopping and marketing gateway layers keep existing Envoy prefix routing.
- BFF aggregators compose catalog, basket, and ordering operations for web and mobile clients.
- SQL Server remains the relational store for identity, catalog, ordering, marketing, and webhooks.
- MongoDB remains the document store for locations and marketing user-location data.
- Redis remains the basket store.
- RabbitMQ remains the default event bus.
- Seq or equivalent structured logging endpoint remains available in local compose.
- Health/status uses Actuator health endpoints and a Spring Boot status UI replacement.

The target system should not become a monolith. Each current application container remains a deployable Spring Boot service unless explicitly listed as shared library code.

## 3. Repository and Module Layout

Create a Maven reactor rooted at the repository root. Use one parent `pom.xml` and one child module per deployable or shared library.

Required modules:

| Module | Type | Purpose |
| --- | --- | --- |
| `common` | Library | Shared DTO helpers, identity claim parsing, exception contracts, Jackson configuration, tracing/logging helpers, validation utilities. |
| `event-bus` | Library | Event abstraction, RabbitMQ publishing/subscription, event name mapping, retry policy, handler dispatch. |
| `integration-event-log` | Library | SQL-backed outbox table and publishing state for services that require transactional event publication. |
| `catalog-api` | Service | Catalog REST and gRPC APIs, catalog SQL schema, seed data, stock validation, price-change publication. |
| `basket-api` | Service | Basket REST and gRPC APIs, Redis basket repository, checkout event publication, product price update handling. |
| `ordering-api` | Service | Ordering REST and gRPC APIs, DDD aggregate, CQRS command handlers, idempotent requests, order saga transitions. |
| `ordering-backgroundtasks` | Service | Grace-period background worker that publishes `GracePeriodConfirmedIntegrationEvent`. |
| `ordering-notifications` | Service | Replacement for SignalR hub using Spring WebSocket/STOMP at `/hub/notificationhub`. |
| `identity-api` | Service | OIDC/OAuth authorization server using Spring Authorization Server. |
| `marketing-api` | Service | Campaign REST API, campaign SQL schema, marketing Mongo repository, user-location event handling. |
| `locations-api` | Service | Locations REST API, MongoDB data ownership, user-location update event publication. |
| `payment-api` | Service | Simulated payment event handler and payment success/failure publication. |
| `webhooks-api` | Service | Webhook subscription REST API, SQL schema, event handlers, outbound webhook delivery. |
| `shopping-aggregator` | Service | Web and mobile shopping aggregator behavior. Deploy as two containers with different service names/config if separate web/mobile images are required. |
| `webmvc` | Service | Spring MVC + Thymeleaf replacement for WebMVC. |
| `webspa` | Service | Spring Boot static host for the Angular SPA. Modernize only as needed for build support while preserving routes and behavior. |
| `webstatus` | Service | Health/status UI replacement backed by Actuator health endpoints. |
| `webhook-client` | Service | Spring MVC + Thymeleaf replacement for webhook demo client. |

Recommended source layout per service:

```text
<module>/
  src/main/java/com/eshop/<module>/
    <Module>Application.java
    api/                  REST controllers and request/response DTOs
    application/          use cases, command handlers, queries
    domain/               domain model, aggregate rules, domain events
    infrastructure/       persistence, messaging, remote clients, security
    config/               Spring configuration
  src/main/resources/
    application.yml
    db/migration/         Flyway or Liquibase migrations where applicable
    proto/                gRPC proto files where applicable
  src/test/java/...
```

Use package names under `com.eshop` and keep per-service packages independent. Shared contracts can live in `common`, but do not share JPA entities across service ownership boundaries.

## 4. Cross-Cutting Implementation Rules

### 4.1 API Compatibility

Use Jackson configuration globally:

- Preserve JSON property names expected by existing clients. Default camelCase is acceptable for most DTOs, but verify against .NET output for exact casing.
- Preserve date/time formats for checkout and ordering payloads. Use `OffsetDateTime` or `Instant` at boundaries and configure Jackson for ISO-8601.
- Preserve decimal money values using `BigDecimal`, not `double`, for REST and persistence.
- Preserve existing status codes, especially `400`, `401`, `403`, `404`, `201`, `202`, and `204`.
- Preserve `x-requestid` header behavior for ordering and checkout idempotency.

### 4.2 Security

All protected APIs must validate JWT bearer tokens issued by `identity-api`.

Implementation:

- `identity-api` uses Spring Authorization Server.
- Resource services use Spring Security OAuth2 Resource Server.
- Server-rendered clients use OAuth2 Login.
- SPA uses browser-compatible OIDC flow. Prefer Authorization Code with PKCE while maintaining equivalent client behavior to the original `js` client.
- Preserve the `sub` claim as the user identity. Do not remap it to another claim name.
- Preserve API scopes:
  `orders`, `basket`, `marketing`, `locations`, `mobileshoppingagg`, `webshoppingagg`, `orders.signalrhub`, and `webhooks`.

Client registrations to preserve:

| Client id | Original use | Spring Authorization Server configuration |
| --- | --- | --- |
| `js` | SPA client | Public client, PKCE, redirect/post-logout to SPA root, scopes `openid`, `profile`, `orders`, `basket`, `locations`, `marketing`, `webshoppingagg`, `orders.signalrhub`, `webhooks`. |
| `xamarin` | Mobile client | Public/native client with PKCE and refresh tokens, redirect URI from `XamarinCallback`, scopes `openid`, `profile`, `offline_access`, `orders`, `basket`, `locations`, `marketing`, `mobileshoppingagg`, `webhooks`. |
| `mvc` | WebMVC | Confidential OAuth2 login client with secret `secret`, redirect `/signin-oidc`, logout callback `/signout-callback-oidc`, 2-hour token lifetimes. |
| `webhooksclient` | Webhook demo client | Confidential OAuth2 login client with secret `secret`, redirect `/signin-oidc`, logout callback `/signout-callback-oidc`, scope `webhooks`. |
| Swagger UI clients | API docs | Public clients with OAuth redirect `/swagger/oauth2-redirect.html` for locations, marketing, basket, ordering, shopping aggregators, and webhooks. |

Seed equivalent demo users and claims from the Identity service setup data. Password hashing can use Spring Security defaults if login behavior remains compatible for seeded users.

### 4.3 Persistence

Keep the original ownership model:

| Service | Store | Java persistence |
| --- | --- | --- |
| `identity-api` | SQL Server | Spring Authorization Server JDBC schema plus app user tables. |
| `catalog-api` | SQL Server | Spring Data JPA or JDBC; migrations for catalog, brands, types, integration event log. |
| `ordering-api` | SQL Server | Spring Data JPA for aggregates plus JDBC queries for read models; migrations for ordering and integration event log. |
| `marketing-api` | SQL Server and MongoDB | JPA for campaigns/rules; Mongo repository for user-location marketing data. |
| `locations-api` | MongoDB | Spring Data MongoDB. |
| `basket-api` | Redis | Spring Data Redis with JSON serialization. |
| `webhooks-api` | SQL Server | Spring Data JPA or JDBC. |

Use schema migrations in every database-owning service. Seed data must reproduce original catalog data, brands, types, locations, ordering card types/status values, campaigns, and identity clients/users.

### 4.4 Event Bus and Outbox

Implement `event-bus` with:

- `IntegrationEvent` base class: `id`, `creationDate`, and event-specific fields.
- `IntegrationEventHandler<T>` interface.
- RabbitMQ publisher using Spring AMQP.
- Subscriber registration by event class.
- Event name convention matching .NET class names without changing routing keys.
- Config values equivalent to `EventBusConnection`, `EventBusUserName`, `EventBusPassword`, `EventBusRetryCount`, `SubscriptionClientName`, and `AzureServiceBusEnabled`.

Implement `integration-event-log` with:

- Table name `IntegrationEventLog`.
- Fields equivalent to event id, event type/name, content JSON, creation time, state, times sent, transaction id.
- States equivalent to not published, in progress, published, and failed.
- Transactional save of business data plus outbox entry for catalog and ordering.
- Publisher that marks entries in progress, published, or failed.

Azure Service Bus support can be deferred behind the same interface. RabbitMQ is required for the first implementation because local compose uses it.

### 4.5 Health, Logging, and Observability

Every service must expose:

- `/hc` for service health.
- `/liveness` for basic liveness if required by deployment scripts.
- Actuator health groups that can be mapped to those paths.
- JSON structured logs with service name and correlation/request id.
- Micrometer metrics.

The status app must read configured service health URLs and display the same logical checks:

- WebMVC
- WebSPA
- Web shopping aggregator
- Mobile shopping aggregator
- Ordering API
- Basket API
- Catalog API
- Identity API
- Marketing API
- Locations API
- Payment API
- Ordering notifications
- Ordering background tasks

## 5. Service Conversion Details

### 5.1 Catalog API

Source area: `src/Services/Catalog/Catalog.API`.

Responsibilities to preserve:

- Catalog item CRUD and query APIs.
- Brand and type query APIs.
- Product picture endpoint.
- Catalog gRPC API.
- SQL Server catalog ownership and seed data.
- Stock validation when orders enter awaiting validation.
- Price-change integration event publication.

Spring implementation:

- `CatalogController` as `@RestController` at `/api/v1/catalog`.
- `PicController` as `@RestController` for `/api/v1/catalog/items/{catalogItemId:int}/pic`.
- JPA entities: `CatalogItem`, `CatalogBrand`, `CatalogType`.
- Domain behavior on `CatalogItem`:
  `removeStock(quantityDesired)` throws domain exception for empty stock or non-positive requested quantity; removes up to available stock.
  `addStock(quantity)` caps at `maxStockThreshold` and clears `onReorder`.
- `CatalogIntegrationEventService` saves catalog changes and outbox entries transactionally.
- gRPC service implements package `CatalogApi`, service `Catalog`.

Public REST API:

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/v1/catalog/items?pageSize={n}&pageIndex={n}` | Return paginated `{pageIndex,pageSize,count,data}` ordered by name. |
| GET | `/api/v1/catalog/items?ids=1,2,3` | Return item list; invalid ids return `400`. |
| GET | `/api/v1/catalog/items/{id}` | Return item, `400` for non-positive id, `404` when missing. |
| GET | `/api/v1/catalog/items/withname/{name}` | Return paginated items whose names start with `name`. |
| GET | `/api/v1/catalog/items/type/{catalogTypeId}/brand/{catalogBrandId}` | Return paginated filtered items. Brand id is optional. |
| GET | `/api/v1/catalog/items/type/all/brand/{catalogBrandId}` | Return paginated items filtered by optional brand id. |
| GET | `/api/v1/catalog/catalogtypes` | Return all catalog types. |
| GET | `/api/v1/catalog/catalogbrands` | Return all catalog brands. |
| PUT | `/api/v1/catalog/items` | Update item; if price changes, publish `ProductPriceChangedIntegrationEvent`; return `201` or `404`. |
| POST | `/api/v1/catalog/items` | Create item and return `201`. |
| DELETE | `/api/v1/catalog/{id}` | Delete item, return `204` or `404`. |
| GET | `/api/v1/catalog/items/{catalogItemId}/pic` | Return product image bytes. |

Required DTO fields:

- Catalog item: `id`, `name`, `description`, `price`, `pictureFileName`, `pictureUri`, `catalogTypeId`, `catalogType`, `catalogBrandId`, `catalogBrand`, `availableStock`, `restockThreshold`, `maxStockThreshold`, `onReorder`.
- Catalog brand: `id`, `brand`.
- Catalog type: `id`, `type`.

Events:

- Publishes `ProductPriceChangedIntegrationEvent(productId, newPrice, oldPrice)`.
- Subscribes to `OrderStatusChangedToAwaitingValidationIntegrationEvent(orderId, orderStockItems)` and publishes either `OrderStockConfirmedIntegrationEvent(orderId)` or `OrderStockRejectedIntegrationEvent(orderId, orderStockItems)`.
- Subscribes to `OrderStatusChangedToPaidIntegrationEvent(orderId, orderStockItems)` to finalize stock effects where applicable.

### 5.2 Basket API

Source area: `src/Services/Basket/Basket.API`.

Responsibilities to preserve:

- Redis-backed customer baskets.
- Basket REST and gRPC APIs.
- Checkout integration event publication.
- Product price change handling.
- Order-started basket cleanup.

Spring implementation:

- `BasketController` as `@RestController` at `/api/v1/basket`.
- `RedisBasketRepository` using Spring Data Redis.
- `IdentityService` reads the authenticated user `sub` claim.
- gRPC service implements package `BasketApi`, service `Basket`.
- Validation rejects basket items with quantity less than 1.

Public REST API:

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| GET | `/api/v1/basket/{id}` | Required | Return basket or empty basket with `buyerId=id`. |
| POST | `/api/v1/basket` | Required | Upsert `CustomerBasket`; return saved basket. |
| POST | `/api/v1/basket/checkout` | Required | Publish checkout event and return `202`; return `400` if authenticated user's basket is missing. |
| DELETE | `/api/v1/basket/{id}` | Required | Delete basket and return `200`. |

Required DTO fields:

- `CustomerBasket`: `buyerId`, `items`.
- `BasketItem`: `id`, `productId`, `productName`, `unitPrice`, `oldUnitPrice`, `quantity`, `pictureUrl`.
- `BasketCheckout`: `city`, `street`, `state`, `country`, `zipCode`, `cardNumber`, `cardHolderName`, `cardExpiration`, `cardSecurityNumber`, `cardTypeId`, `buyer`, `requestId`.

Events:

- Publishes `UserCheckoutAcceptedIntegrationEvent` with user id, user name, address, card data, buyer, request id, and basket.
- Subscribes to `ProductPriceChangedIntegrationEvent`; update matching basket item prices and set `oldUnitPrice`.
- Subscribes to `OrderStartedIntegrationEvent`; delete the buyer basket after order creation starts.

### 5.3 Ordering API

Source area: `src/Services/Ordering/Ordering.API`, `Ordering.Domain`, and `Ordering.Infrastructure`.

Responsibilities to preserve:

- DDD ordering aggregate.
- CQRS command handling.
- Idempotent command processing using `x-requestid`.
- Ordering read models.
- Order draft generation.
- Ordering gRPC API.
- Transactional outbox publication.
- Order lifecycle event handling.

Spring implementation:

- `OrdersController` as `@RestController` at `/api/v1/orders`.
- Command classes as immutable Java records/classes where practical.
- Command handlers as Spring services.
- Replace MediatR with explicit command bus abstraction or direct handler injection. Use a simple local `CommandHandler<C,R>` registry if controller/handler decoupling is desired.
- Use JPA entities for aggregate persistence, preserving encapsulated behavior.
- Use JDBC or projection repositories for query/read models.
- Implement `ClientRequest` idempotency table equivalent for `IdentifiedCommand`.
- gRPC service implements package `OrderingApi`, service `OrderingGrpc`.

Public REST API:

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| PUT | `/api/v1/orders/cancel` | Required | Body `CancelOrderCommand`, header `x-requestid`; return `200` or `400`. |
| PUT | `/api/v1/orders/ship` | Required | Body `ShipOrderCommand`, header `x-requestid`; return `200` or `400`. |
| GET | `/api/v1/orders/{orderId}` | Required | Return order detail or `404`. |
| GET | `/api/v1/orders` | Required | Return orders for authenticated user. |
| GET | `/api/v1/orders/cardtypes` | Required | Return card types. |
| POST | `/api/v1/orders/draft` | Required | Create draft from basket data and return draft total/items. |

Ordering aggregate rules:

- Initial status is `submitted`.
- Status values must keep ids and names:
  `1=submitted`, `2=awaitingvalidation`, `3=stockconfirmed`, `4=paid`, `5=shipped`, `6=cancelled`.
- `setAwaitingValidationStatus()` only transitions from submitted and emits awaiting-validation domain event.
- `setStockConfirmedStatus()` only transitions from awaiting validation.
- `setPaidStatus()` only transitions from stock confirmed.
- `setShippedStatus()` only transitions from paid; otherwise throw a domain exception.
- `setCancelledStatus()` must reject cancellation after paid or shipped.
- `setCancelledStatusWhenStockIsRejected()` cancels only from awaiting validation and includes rejected product names in description.
- `addOrderItem()` merges items by product id, keeps the greater discount, and increments units.

Events:

- Subscribes to:
  `UserCheckoutAcceptedIntegrationEvent`, `GracePeriodConfirmedIntegrationEvent`, `OrderStockConfirmedIntegrationEvent`, `OrderStockRejectedIntegrationEvent`, `OrderPaymentFailedIntegrationEvent`, `OrderPaymentSucceededIntegrationEvent`.
- Publishes:
  `OrderStartedIntegrationEvent`, `OrderStatusChangedToSubmittedIntegrationEvent`, `OrderStatusChangedToAwaitingValidationIntegrationEvent`, `OrderStatusChangedToStockConfirmedIntegrationEvent`, `OrderStatusChangedToPaidIntegrationEvent`, `OrderStatusChangedToShippedIntegrationEvent`, `OrderStatusChangedToCancelledIntegrationEvent`.

### 5.4 Ordering Background Tasks

Source area: `src/Services/Ordering/Ordering.BackgroundTasks`.

Responsibilities to preserve:

- Periodically find submitted orders whose grace period has elapsed.
- Publish `GracePeriodConfirmedIntegrationEvent(orderId)`.
- Honor `CheckUpdateTime` and `GracePeriodTime` configuration.

Spring implementation:

- Spring Boot service with `@Scheduled(fixedDelayString = ...)`.
- Use JDBC/JPA query against ordering database.
- Publish integration events via `event-bus`.
- Expose `/hc`.

### 5.5 Ordering Notifications

Source area: `src/Services/Ordering/Ordering.SignalrHub`.

Responsibilities to preserve:

- Notify clients when order status changes.
- Remain reachable through Envoy at `/hub/notificationhub`.
- Subscribe to order status integration events.

Spring implementation:

- Use Spring WebSocket with STOMP endpoint `/hub/notificationhub`.
- Converted MVC/SPA clients must use STOMP/WebSocket instead of Microsoft SignalR client.
- Subscribe to:
  `OrderStatusChangedToAwaitingValidationIntegrationEvent`,
  `OrderStatusChangedToPaidIntegrationEvent`,
  `OrderStatusChangedToStockConfirmedIntegrationEvent`,
  `OrderStatusChangedToShippedIntegrationEvent`,
  `OrderStatusChangedToCancelledIntegrationEvent`,
  `OrderStatusChangedToSubmittedIntegrationEvent`.
- Publish messages to user-specific destinations preserving the original user-visible behavior.
- Keep scope/resource name `orders.signalrhub` for authorization.

### 5.6 Payment API

Source area: `src/Services/Payment/Payment.API`.

Responsibilities to preserve:

- Simulated payment processing.
- Consume stock-confirmed order events.
- Publish payment success or failure events.

Spring implementation:

- No public business REST endpoints are required beyond health.
- Event handler for `OrderStatusChangedToStockConfirmedIntegrationEvent`.
- Publish `OrderPaymentSucceededIntegrationEvent(orderId)` for the normal happy path.
- Preserve the failure path and `OrderPaymentFailedIntegrationEvent(orderId)` contract for tests and future behavior.
- Expose `/hc`.

### 5.7 Identity API

Source area: `src/Services/Identity/Identity.API`.

Responsibilities to preserve:

- OIDC/OAuth authorization server.
- Login, logout, consent, seeded users, clients, scopes, API resources.
- External URLs configured from compose variables.
- Token issuance compatible with resource services and web clients.

Spring implementation:

- Use Spring Authorization Server.
- Use Spring Security form login and Thymeleaf templates for login/consent pages.
- Store registered clients and authorizations in SQL Server through JDBC.
- Seed clients listed in section 4.2.
- Map original app settings to Spring properties:
  `SpaClient`, `XamarinCallback`, `MvcClient`, `LocationApiClient`, `MarketingApiClient`, `BasketApiClient`, `OrderingApiClient`, `MobileShoppingAggClient`, `WebShoppingAggClient`, `WebhooksApiClient`, `WebhooksWebClient`.
- Preserve issuer behavior for internal and external URLs. Resource services must validate tokens using the configured external issuer or an issuer alias strategy suitable for Docker.

OIDC/OAuth endpoints should follow Spring Authorization Server defaults:

- `/.well-known/openid-configuration`
- `/oauth2/authorize`
- `/oauth2/token`
- `/oauth2/jwks`
- `/connect/logout` or a compatibility logout endpoint if clients expect IdentityServer-style logout.

If exact IdentityServer endpoint names are needed by existing mobile clients, add compatibility routes that delegate to the Spring Authorization Server endpoints.

### 5.8 Marketing API

Source area: `src/Services/Marketing/Marketing.API`.

Responsibilities to preserve:

- Campaign CRUD.
- Campaign picture endpoint.
- Campaign location-rule APIs.
- Personalized campaign lookup based on user locations.
- SQL ownership for campaigns/rules.
- Mongo marketing data repository for user location data.
- Consume user-location update events.

Spring implementation:

- `CampaignsController` at `/api/v1/campaigns`.
- `LocationsController` preserving nested campaign location-rule paths.
- JPA entities: `Campaign`, `Rule`, `UserLocationRule`, `RuleType`.
- Mongo document equivalent for user marketing location data.
- Preserve details URI generation from `CampaignDetailFunctionUri`.
- Preserve picture URL generation from `PicBaseUrl` and `AzureStorageEnabled`.

Public REST API:

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| GET | `/api/v1/campaigns` | Required | Return all campaigns. |
| GET | `/api/v1/campaigns/{id}` | Required | Return campaign or `404`. |
| POST | `/api/v1/campaigns` | Required | Create campaign, return `201` or `400`. |
| PUT | `/api/v1/campaigns/{id}` | Required | Update campaign, return `201`, `400`, or `404`. |
| DELETE | `/api/v1/campaigns/{id}` | Required | Delete campaign, return `204`, `400`, or `404`. |
| GET | `/api/v1/campaigns/user?pageSize={n}&pageIndex={n}` | Required | Return paginated campaigns for authenticated user's locations. |
| GET | `/api/v1/campaigns/{campaignId}/locations/{userLocationRuleId}` | Required | Return rule, `400`, or `404`. |
| GET | `/api/v1/campaigns/{campaignId}/locations` | Required | Return location rules. |
| POST | `/api/v1/campaigns/{campaignId}/locations` | Required | Create rule, return `201` or `400`. |
| DELETE | `/api/v1/campaigns/{campaignId}/locations/{userLocationRuleId}` | Required | Delete rule, return `204`, `400`, or `404`. |
| GET | `/api/v1/campaigns/{campaignId}/pic` | Public or same as source | Return campaign image. |

Events:

- Subscribes to `UserLocationUpdatedIntegrationEvent(userId, locationList)` and updates marketing Mongo data.

### 5.9 Locations API

Source area: `src/Services/Location/Locations.API`.

Responsibilities to preserve:

- Mongo-backed location catalog and user locations.
- User location create/update.
- Publish user-location update events.

Spring implementation:

- `LocationsController` at `/api/v1/locations`.
- Mongo documents: `Locations`, `UserLocation`, `UserLocationDetails`, `LocationPoint`, `LocationPolygon`.
- `LocationsService` handles repository access and event publication.

Public REST API:

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| GET | `/api/v1/locations/user/{userId}` | Required | Return user location document. |
| GET | `/api/v1/locations` | Required | Return all known locations. |
| GET | `/api/v1/locations/{locationId}` | Required | Return one location. |
| POST | `/api/v1/locations` | Required | Create or update authenticated user's location, return `200` or `400`. |

Events:

- Publishes `UserLocationUpdatedIntegrationEvent(userId, locationList)`.

### 5.10 Webhooks API

Source area: `src/Services/Webhooks/Webhooks.API`.

Responsibilities to preserve:

- Webhook subscription CRUD for authenticated users.
- Grant URL validation before subscription creation.
- Consume integration events and deliver webhook callbacks.
- SQL ownership for subscriptions.

Spring implementation:

- `WebhooksController` at `/api/v1/webhooks`.
- JPA entity `WebhookSubscription`.
- Enum `WebhookType` preserving source values.
- `GrantUrlTesterService` posts/gets to grant URL with token behavior equivalent to source.
- `WebhooksSender` sends outbound webhook payloads and token header.
- Use async HTTP client with retry/backoff for callback delivery.

Public REST API:

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| GET | `/api/v1/webhooks` | Required | List current user's subscriptions. |
| GET | `/api/v1/webhooks/{id}` | Required | Return current user's subscription or `404`. |
| POST | `/api/v1/webhooks` | Required | Validate grant URL, create subscription and return `201`; invalid model returns validation problem; failed grant returns HTTP `418`. |
| DELETE | `/api/v1/webhooks/{id}` | Required | Delete current user's subscription, return `202` or `404`. |

Events:

- Subscribes to `ProductPriceChangedIntegrationEvent`.
- Subscribes to `OrderStatusChangedToShippedIntegrationEvent`.
- Subscribes to `OrderStatusChangedToPaidIntegrationEvent`.
- Sends webhook data to subscribers matching the event type.

### 5.11 Shopping Aggregator

Source areas:

- `src/ApiGateways/Web.Bff.Shopping/aggregator`
- `src/ApiGateways/Mobile.Bff.Shopping/aggregator`

Responsibilities to preserve:

- Compose catalog, basket, and ordering calls for shopping clients.
- Preserve web and mobile aggregator deployment names and auth scopes.
- Use gRPC for basket/catalog/ordering where source uses gRPC clients.

Spring implementation:

- Implement one `shopping-aggregator` codebase.
- Deploy twice if compose requires both `webshoppingagg` and `mobileshoppingagg`.
- Configure service name and OAuth scope per deployment:
  `webshoppingagg` and `mobileshoppingagg`.
- Use Spring WebClient for REST calls and generated gRPC clients for gRPC calls.

Public REST API:

| Method | Path | Auth | Behavior |
| --- | --- | --- | --- |
| POST/PUT | `/api/v1/basket` | Required | Update whole basket from item list, merging duplicate products and resolving catalog data. |
| PUT | `/api/v1/basket/items` | Required | Update basket item quantities. |
| POST | `/api/v1/basket/items` | Required | Add one catalog item to basket. |
| GET | `/api/v1/order/draft/{basketId}` | Required | Build order draft from basket. |

Remote URL config must preserve:

- `urls.basket`
- `urls.catalog`
- `urls.orders`
- `urls.identity`
- `urls.grpcBasket`
- `urls.grpcCatalog`
- `urls.grpcOrdering`

### 5.12 WebMVC

Source area: `src/Web/WebMVC`.

Responsibilities to preserve:

- Server-rendered shopping web app.
- Login/logout through identity service.
- Catalog browsing, basket management, checkout, order detail/history, campaigns, user location updates, order management actions.
- Existing public routes and visual behavior where practical.

Spring implementation:

- Spring MVC controllers and Thymeleaf templates.
- OAuth2 Login client `mvc`.
- Store access/refresh tokens in the authenticated session.
- Add bearer token to downstream gateway requests.
- Add `x-requestid` to order-changing requests.
- Preserve app settings:
  `PurchaseUrl`, `MarketingUrl`, `IdentityUrl`, `CallBackUrl`, `SignalrHubUrl`, `IdentityUrlHC`, `UseCustomizationData`, `UseLoadTest`.

Controller behavior to preserve:

- `CatalogController.Index`: filter by brand/type, paginate 10 items per page.
- `CartController`: view basket, update quantities, checkout or continue shopping actions, add to cart.
- `OrderController`: create order draft, checkout, cancel, detail, order history.
- `OrderManagementController`: list orders and ship/cancel actions.
- `CampaignsController`: list campaigns, details, create/update user location.
- `AccountController`: sign in and sign out.

### 5.13 WebSPA

Source area: `src/Web/WebSPA`.

Responsibilities to preserve:

- SPA routes:
  `/catalog`, `/basket`, `/orders`, `/orders/:id`, `/order`, `/campaigns`, `/campaigns/:id`.
- OIDC login behavior.
- Catalog, basket, order, campaign, and notification flows.
- Static web app hosted from container port `5104`.

Spring implementation:

- Spring Boot service serves built SPA assets.
- Keep SPA route fallback to `index.html`.
- Modernize Angular dependencies only as needed to build and run safely, but preserve UI routes and service behavior.
- Replace SignalR client usage with STOMP/WebSocket client pointed at `/hub/notificationhub`.
- Preserve config endpoint or static config equivalent for:
  `IdentityUrl`, `PurchaseUrl`, `MarketingUrl`, `SignalrHubUrl`, and health URLs.

### 5.14 WebStatus

Source area: `src/Web/WebStatus`.

Responsibilities to preserve:

- Redirect root to health UI.
- Expose `/Config` view of configured health checks.
- Monitor all services listed in compose.

Spring implementation:

- Spring MVC controller:
  root redirects to `/hc-ui`.
  `/Config` renders configured health checks.
- Periodic health polling using WebClient or server-side page refresh.
- Read configuration compatible with `HealthChecksUI__HealthChecks__{n}__Name` and `HealthChecksUI__HealthChecks__{n}__Uri` environment variables or provide a documented Spring relaxed-binding mapping.

### 5.15 Webhook Client

Source area: `src/Web/WebhookClient`.

Responsibilities to preserve:

- OAuth2 login as `webhooksclient`.
- Register webhook subscriptions.
- Receive webhook callbacks.
- Show received webhook list from in-memory repository.
- Validate callback token when configured.

Spring implementation:

- Spring MVC + Thymeleaf.
- OAuth2 Login client `webhooksclient`.
- In-memory repository for received hooks.
- `POST /webhook-received` accepts webhook payload and token.
- Register page posts subscription request to `webhooks-api`.
- Preserve settings:
  `Token`, `IdentityUrl`, `CallBackUrl`, `WebhooksUrl`, `SelfUrl`, `ValidateToken`.

## 6. Public API Appendix

### 6.1 Gateway Prefixes

Keep Envoy routes and prefix rewrites:

| Gateway | External prefix | Internal target |
| --- | --- | --- |
| web/mobile shopping | `/c/` | `/catalog-api/` on `catalog-api` |
| web/mobile shopping | `/catalog-api/` | `catalog-api` |
| web/mobile shopping | `/b/` | `/basket-api/` on `basket-api` |
| web/mobile shopping | `/basket-api/` | `basket-api` |
| web/mobile shopping | `/o/` | `/ordering-api/` on `ordering-api` |
| web/mobile shopping | `/ordering-api/` | `ordering-api` |
| web/mobile shopping | `/hub/notificationhub` | `ordering-notifications` |
| web/mobile shopping | `/` | `shopping-aggregator` |
| web/mobile marketing | `/m/` | `/marketing-api/` on `marketing-api` |
| web/mobile marketing | `/marketing-api/` | `marketing-api` |
| web marketing | `/l/` | `/locations-api/` on `locations-api` |
| web marketing | `/locations-api/` | `locations-api` |

### 6.2 Ports

Preserve local compose ports:

| Container | External ports |
| --- | --- |
| `identity-api` | `5105:80` |
| `catalog-api` | `5101:80`, `9101:81` |
| `ordering-api` | `5102:80`, `9102:81` |
| `basket-api` | `5103:80`, `9103:81` |
| `webspa` | `5104:80` |
| `webmvc` | `5100:80` |
| `webstatus` | `5107:80` |
| `payment-api` | `5108:80` |
| `locations-api` | `5109:80` |
| `marketing-api` | `5110:80` |
| `ordering-backgroundtasks` | `5111:80` |
| `ordering-notifications` | `5112:80` |
| `webhooks-api` | `5113:80` |
| `webhook-client` | `5114:80` |
| `mobileshoppingagg` | `5120:80` |
| `webshoppingagg` | `5121:80` |
| `mobileshoppingapigw` | `5200:80`, `15200:8001` |
| `mobilemarketingapigw` | `5201:80`, `15201:8001` |
| `webshoppingapigw` | `5202:80`, `15202:8001` |
| `webmarketingapigw` | `5203:80`, `15203:8001` |
| `seq` | `5340:80` |
| `sqldata` | `5433:1433` |
| `basketdata` | `6379:6379` |
| `nosqldata` | `27017:27017` |
| `rabbitmq` | `5672:5672`, `15672:15672` |

Inside containers, HTTP services should listen on port `80`; gRPC services should listen on port `81`.

### 6.3 gRPC Contracts

Preserve proto package, service, rpc, and message field names/numbers.

Catalog:

```proto
package CatalogApi;
service Catalog {
  rpc GetItemById (CatalogItemRequest) returns (CatalogItemResponse);
  rpc GetItemsByIds (CatalogItemsRequest) returns (PaginatedItemsResponse);
}
```

Basket:

```proto
package BasketApi;
service Basket {
  rpc GetBasketById(BasketRequest) returns (CustomerBasketResponse);
  rpc UpdateBasket(CustomerBasketRequest) returns (CustomerBasketResponse);
}
```

Ordering:

```proto
package OrderingApi;
service OrderingGrpc {
  rpc CreateOrderDraftFromBasketData(CreateOrderDraftCommand) returns (OrderDraftDTO);
}
```

Generated Java code must not change the wire schema. Put copied proto files under the owning service resources and generate stubs during Maven builds.

### 6.4 Required Headers

| Header | Use |
| --- | --- |
| `Authorization: Bearer <token>` | All protected APIs and downstream service calls. |
| `x-requestid` | Idempotent checkout, ordering cancel, ordering ship, and order creation flows. Must be a non-empty GUID/UUID. |
| Webhook token header | Preserve the source callback token behavior in webhook API/client. |

### 6.5 Health Endpoints

Every service must expose `/hc`. Map to Actuator health with custom groups if necessary. Existing compose and status configuration should continue to work without changing the monitored URLs.

## 7. Event Contract Appendix

Use JSON payloads with the same public field names as the .NET events. Event names/routing keys must match the source event class names.

Core events:

| Event | Publisher | Subscribers | Required payload |
| --- | --- | --- | --- |
| `ProductPriceChangedIntegrationEvent` | Catalog | Basket, Webhooks | `productId`, `newPrice`, `oldPrice`. |
| `UserCheckoutAcceptedIntegrationEvent` | Basket | Ordering | `userId`, `userName`, address fields, card fields, `cardTypeId`, `buyer`, `requestId`, `basket`. |
| `OrderStartedIntegrationEvent` | Ordering | Basket | `userId`. |
| `GracePeriodConfirmedIntegrationEvent` | Ordering background tasks | Ordering | `orderId`. |
| `OrderStatusChangedToAwaitingValidationIntegrationEvent` | Ordering | Catalog, Notifications | `orderId`, `orderStockItems[{productId,units}]`. |
| `OrderStockConfirmedIntegrationEvent` | Catalog | Ordering | `orderId`. |
| `OrderStockRejectedIntegrationEvent` | Catalog | Ordering | `orderId`, rejected stock items. |
| `OrderStatusChangedToStockConfirmedIntegrationEvent` | Ordering | Payment, Notifications | `orderId`. |
| `OrderPaymentSucceededIntegrationEvent` | Payment | Ordering | `orderId`. |
| `OrderPaymentFailedIntegrationEvent` | Payment | Ordering | `orderId`. |
| `OrderStatusChangedToPaidIntegrationEvent` | Ordering | Catalog, Webhooks, Notifications | `orderId`, order stock/items as source event requires. |
| `OrderStatusChangedToShippedIntegrationEvent` | Ordering | Webhooks, Notifications | `orderId`. |
| `OrderStatusChangedToCancelledIntegrationEvent` | Ordering | Notifications | `orderId`. |
| `OrderStatusChangedToSubmittedIntegrationEvent` | Ordering | Notifications | `orderId`. |
| `UserLocationUpdatedIntegrationEvent` | Locations | Marketing | `userId`, `locationList`. |

Event handling requirements:

- Handlers must be idempotent where repeated messages can be observed.
- Message publishing must be retried according to `EventBusRetryCount`.
- Failed event publication must leave outbox entries in failed or not-published state for retry.
- Do not publish events before the local database transaction commits.

## 8. Docker Compose and Configuration

Create Spring equivalents of the source Dockerfiles and compose definitions.

Keep infrastructure containers:

- `sqldata` using SQL Server.
- `nosqldata` using MongoDB.
- `basketdata` using Redis.
- `rabbitmq`.
- `seq`.
- Envoy gateways and config files.

Replace .NET app images with Java images:

- Image names should retain the original logical names where practical, for example `${REGISTRY:-eshop}/catalog.api:${PLATFORM:-linux}-${TAG:-latest}` can point to the Java catalog image.
- Keep service names in compose unchanged so Envoy and service discovery continue to resolve:
  `identity-api`, `basket-api`, `catalog-api`, `ordering-api`, `ordering-backgroundtasks`, `marketing-api`, `payment-api`, `locations-api`, `webhooks-api`, `mobileshoppingagg`, `webshoppingagg`, `ordering-signalrhub` or renamed container alias `ordering-notifications`, `webstatus`, `webspa`, `webmvc`, `webhooks-client`.

Spring environment mapping examples:

| Source variable | Spring property |
| --- | --- |
| `ConnectionString` | `spring.datasource.url`, service-specific datasource config. |
| `MongoConnectionString` / `ConnectionString` for Mongo services | `spring.data.mongodb.uri`. |
| `MongoDatabase` / `Database` | `spring.data.mongodb.database`. |
| `EventBusConnection` | `eshop.eventbus.host`. |
| `EventBusUserName` | `spring.rabbitmq.username`. |
| `EventBusPassword` | `spring.rabbitmq.password`. |
| `EventBusRetryCount` | `eshop.eventbus.retry-count`. |
| `IdentityUrlExternal` | `eshop.security.identity-url-external`. |
| `identityUrl` | `eshop.security.identity-url-internal`. |
| `PATH_BASE` | `server.servlet.context-path` or gateway-aware base path handling. |
| `GRPC_PORT` | gRPC server port. |
| `PORT` | `server.port`. |

Path base handling:

- Preserve source `PATH_BASE` values such as `/catalog-api`, `/basket-api`, `/ordering-api`, `/marketing-api`, and `/locations-api`.
- Services must work both directly on root paths and behind Envoy prefix rewrites where the source supports both.

## 9. Testing Strategy and Acceptance Criteria

### 9.1 Unit Tests

Required unit coverage:

- Catalog stock add/remove behavior and domain exceptions.
- Basket quantity validation and price-change handling.
- Ordering aggregate status transitions and invalid transitions.
- Ordering idempotent command handling.
- Marketing campaign/rule mapping.
- Webhook subscription validation and grant URL validation.
- Event name mapping and serialization.

### 9.2 Contract Tests

Create contract tests that compare Java behavior to the source .NET behavior for public interfaces.

REST contract coverage:

- All endpoints listed in this document.
- Status codes and validation responses.
- Auth-required versus anonymous behavior.
- JSON property names and data types.
- Pagination shape: `pageIndex`, `pageSize`, `count`, `data`.
- `x-requestid` behavior for idempotent command endpoints.

gRPC contract coverage:

- Generate client stubs from preserved proto files.
- Test catalog item lookup and multi-item lookup.
- Test basket get/update.
- Test ordering draft generation.

OAuth contract coverage:

- Discovery document exists.
- MVC and webhook client login flows work.
- SPA public client can authenticate.
- JWT contains expected scopes and `sub`.
- Protected APIs reject missing/invalid tokens.

### 9.3 Integration Tests

Use Testcontainers for:

- SQL Server.
- MongoDB.
- Redis.
- RabbitMQ.

Required flows:

- Catalog seed data loads and image endpoint returns bytes.
- Basket update persists to Redis.
- Basket checkout publishes `UserCheckoutAcceptedIntegrationEvent`.
- Ordering consumes checkout and creates submitted order.
- Background task publishes grace-period event.
- Catalog confirms or rejects stock.
- Payment publishes payment success.
- Ordering transitions to paid.
- Notifications publish status updates to WebSocket/STOMP clients.
- Webhooks API receives paid/shipped events and delivers callbacks.
- Locations API publishes user location update and Marketing API updates personalized campaign data.

### 9.4 End-to-End Acceptance Tests

Run against Docker Compose:

1. Browse WebMVC at `http://host.docker.internal:5100/`.
2. Browse WebSPA at `http://host.docker.internal:5104/`.
3. Open WebStatus at `http://host.docker.internal:5107/` and verify all service health checks pass.
4. Login through Identity from MVC and SPA.
5. Browse catalog, filter by brand/type, and view product images.
6. Add item to basket, update quantity, and checkout.
7. Verify ordering saga reaches paid through stock and payment events.
8. Cancel an eligible order and ship a paid order through order management.
9. Create/update user location and verify personalized campaign list changes.
10. Register a webhook, trigger a subscribed event, and verify webhook client receives it.

Migration acceptance:

- Original compose URLs and exposed ports remain usable.
- Envoy routes preserve prefix behavior.
- Mobile-facing APIs remain compatible.
- Java containers replace .NET containers without requiring client URL changes.
- All required tests pass in CI.

## 10. Implementation Phasing

Recommended build order:

1. Create Maven parent, shared libraries, Docker base image, and common configuration conventions.
2. Implement `identity-api` enough for token issuance and resource-server validation.
3. Implement `event-bus` and `integration-event-log`.
4. Implement `catalog-api`, including seed data, REST, gRPC, and price-change event.
5. Implement `basket-api`, including Redis, REST, gRPC, and checkout event.
6. Implement `ordering-api`, including aggregate, commands, queries, outbox, and gRPC draft.
7. Implement `ordering-backgroundtasks`, `payment-api`, and stock/payment saga event flow.
8. Implement `locations-api` and `marketing-api`.
9. Implement `webhooks-api` and `webhook-client`.
10. Implement `shopping-aggregator` and Envoy integration.
11. Convert `webmvc`, host/update `webspa`, and implement `ordering-notifications`.
12. Implement `webstatus`, compose integration, full E2E tests, and CI.

Each phase must include tests before moving to the next phase. Do not wait until the final phase to validate event contracts.

## 11. Key Risks and Decisions

| Risk | Decision or mitigation |
| --- | --- |
| IdentityServer to Spring Authorization Server differences | Preserve OIDC/OAuth client behavior and add compatibility endpoints where existing clients require IdentityServer-style paths. |
| SignalR protocol not available natively in Spring | Use Spring WebSocket/STOMP at `/hub/notificationhub`; converted UI clients use STOMP. |
| Event payload casing drift | Add serialization contract tests against captured .NET payload examples. |
| EF Core private-field aggregate mapping differences | Keep domain behavior in Java methods and map JPA carefully with field access where needed. |
| SQL Server schema drift | Use migrations and seed scripts reviewed against source migrations and seed files. |
| Docker issuer URL mismatch | Support internal/external identity URLs and document the issuer used by resource servers. |
| SPA dependency age | Modernize only enough for supported builds and security while preserving route and API behavior. |

## 12. Done Definition

The conversion is complete when:

- All modules build with `mvn verify`.
- Docker Compose starts the complete Java-based application stack.
- WebMVC, WebSPA, WebStatus, and Webhook Client are reachable on the original ports.
- Public REST and gRPC contract tests pass.
- OIDC/OAuth login works for MVC, SPA, mobile-compatible client configuration, Swagger clients, and webhook client.
- The checkout/order saga works through RabbitMQ with SQL outbox guarantees.
- Catalog, basket, ordering, marketing, locations, payment, webhooks, notifications, and status features match the source behavior.
- No client-facing URL, route, gateway prefix, or compose port changes are required for the documented public surface.
