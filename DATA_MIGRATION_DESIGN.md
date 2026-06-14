# Data Migration Design

## 1. Purpose

This document defines how the Spring Boot conversion of eShopOnContainers will replicate the data schema and initial data loading behavior from the sibling .NET application.

The source baseline is:

- Source repo: `C:\Projects\eShopOnContainers`
- Branch: `main` / `origin/main`
- Commit: `31ab9b62`
- Target repo: `C:\Projects\Spring-eShop`
- Target stack: Java 21, Maven multi-module, Spring Boot 4.x, SQL Server, MongoDB, Redis, RabbitMQ, Flyway, Spring Data JPA/JDBC, Spring Data MongoDB, Spring Data Redis, Spring Authorization Server, Testcontainers

The migration scope is schema plus reference/demo seed data. It does not include live production order history, user-generated baskets, user locations, webhook registrations, or persisted authorization grants unless a later production migration is requested.

## 2. Migration Principles

1. Preserve service-owned data boundaries.
   Each Spring service owns the same database or store as the .NET service it replaces. No cross-service shared tables are introduced.

2. Preserve public data contracts.
   JSON casing, ids exposed by APIs, enum values, event payload fields, image file names, and seed values remain compatible with the .NET application.

3. Preserve final source schema behavior.
   The Spring schema must reflect the final state of the .NET EF Core migrations, not only the entity classes.

4. Use deterministic initial loading.
   Seed scripts and runners must be idempotent. Re-running a service against an already seeded database must not duplicate reference rows, reset user-created rows, or rewrite runtime data.

5. Use Spring-native runtime infrastructure where required.
   Spring Authorization Server uses its own runtime tables for registered clients, authorizations, and consents. The source IdentityServer client/resource seed data is mapped into those tables while preserving the same client ids, secrets, redirect URIs, scopes, and token behavior.

6. Keep source-compatible logical data models.
   Where the Java implementation uses different table names internally, it must expose equivalent API behavior and preserve source-compatible data values. For identity users, keep a source-compatible profile table because the .NET user seed contains payment and address fields used by the application.

## 3. Authoritative Source Inventory

Use these .NET files as the source of truth when implementing migrations and seed loaders.

| Area | Source files | Spring module |
| --- | --- | --- |
| Catalog schema | `src\Services\Catalog\Catalog.API\Infrastructure\CatalogContext.cs`, `Infrastructure\EntityConfigurations\*.cs`, `Infrastructure\CatalogMigrations\*.cs` | `catalog-api` |
| Catalog seed | `src\Services\Catalog\Catalog.API\Infrastructure\CatalogContextSeed.cs`, `Setup\CatalogBrands.csv`, `Setup\CatalogTypes.csv`, `Setup\CatalogItems.csv`, `Setup\CatalogItems.zip` | `catalog-api` |
| Ordering schema | `src\Services\Ordering\Ordering.Infrastructure\OrderingContext.cs`, `EntityConfigurations\*.cs`, `OrderingMigrations\*.cs` | `ordering-api` |
| Ordering seed | `src\Services\Ordering\Ordering.API\Infrastructure\OrderingContextSeed.cs` | `ordering-api` |
| Integration event log | `src\BuildingBlocks\EventBus\IntegrationEventLogEF\IntegrationEventLogContext.cs`, `IntegrationEventLogEntry.cs`, per-service integration event migrations | `integration-event-log`, SQL-backed services |
| Identity users | `src\Services\Identity\Identity.API\Data\ApplicationDbContext.cs`, `ApplicationDbContextSeed.cs`, `Models\ApplicationUser.cs`, `Setup\Users.csv`, `Setup\images.zip` | `identity-api` |
| Identity clients/resources | `src\Services\Identity\Identity.API\Data\ConfigurationDbContextSeed.cs`, `Configuration\Config.cs`, IdentityServer EF migrations | `identity-api` |
| Marketing SQL | `src\Services\Marketing\Marketing.API\Infrastructure\MarketingContext.cs`, `EntityConfigurations\*.cs`, `MarketingMigrations\*.cs`, `MarketingContextSeed.cs` | `marketing-api` |
| Marketing Mongo read model | `src\Services\Marketing\Marketing.API\Infrastructure\MarketingReadDataContext.cs`, `Model\MarketingData.cs` | `marketing-api` |
| Locations Mongo | `src\Services\Location\Locations.API\Infrastructure\LocationsContext.cs`, `LocationsContextSeed.cs`, `Model\Locations.cs`, `Model\UserLocation.cs` | `locations-api` |
| Basket Redis | `src\Services\Basket\Basket.API\Infrastructure\Repositories\RedisBasketRepository.cs`, `Model\CustomerBasket.cs`, `Model\BasketItem.cs`, `Model\BasketCheckout.cs` | `basket-api` |
| Webhooks schema | `src\Services\Webhooks\Webhooks.API\Infrastructure\WebhooksContext.cs`, `Migrations\20190118091148_Initial.cs`, `Model\WebhookType.cs`, `Model\WebhookSubscription.cs` | `webhooks-api` |

## 4. Target Migration Layout

Each persistent service owns its migrations under its module.

```text
catalog-api/
  src/main/resources/db/migration/
    V001__catalog_schema.sql
    V002__catalog_reference_data.sql
  src/main/resources/setup/
    CatalogBrands.csv
    CatalogTypes.csv
    CatalogItems.csv
    CatalogItems.zip

ordering-api/
  src/main/resources/db/migration/
    V001__ordering_schema.sql
    V002__ordering_reference_data.sql

identity-api/
  src/main/resources/db/migration/
    V001__identity_user_schema.sql
    V002__spring_authorization_server_schema.sql
    V003__identity_reference_data.sql
  src/main/resources/setup/
    Users.csv
    images.zip

marketing-api/
  src/main/resources/db/migration/
    V001__marketing_schema.sql
    V002__marketing_reference_data.sql

webhooks-api/
  src/main/resources/db/migration/
    V001__webhooks_schema.sql

integration-event-log/
  src/main/resources/db/migration/
    R__integration_event_log.sql

locations-api/
  src/main/resources/setup/
    locations-seed.json

basket-api/
  no SQL migrations; Redis schema is documented JSON compatibility
```

Flyway is enabled only for SQL-backed services. MongoDB seed data is loaded by Spring beans guarded by collection existence checks and unique indexes. Redis basket data is runtime-only and loaded only by API actions.

## 5. SQL Server Databases

The Java services must keep the same logical databases used by the .NET Docker Compose architecture.

| Service | Database |
| --- | --- |
| Catalog | `Microsoft.eShopOnContainers.Services.CatalogDb` |
| Ordering | `Microsoft.eShopOnContainers.Services.OrderingDb` |
| Identity | `Microsoft.eShopOnContainers.Service.IdentityDb` |
| Marketing | `Microsoft.eShopOnContainers.Services.MarketingDb` |
| Webhooks | `Microsoft.eShopOnContainers.Services.WebhooksDb` |

Local development may use a single SQL Server instance with these databases. The service connection strings must point to only the database owned by that service.

## 6. Catalog Data Design

### 6.1 Schema

The catalog service owns these SQL objects in `Microsoft.eShopOnContainers.Services.CatalogDb`.

`CatalogBrand`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `catalogbrand_hilo` |
| `Brand` | `nvarchar(100)` | Required |

`CatalogType`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `catalogtype_hilo` |
| `Type` | `nvarchar(100)` | Required |

`Catalog`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `catalog_hilo` |
| `Name` | `nvarchar(50)` | Required |
| `Description` | `nvarchar(max)` | Required by data model |
| `Price` | `decimal(18,2)` | Required |
| `PictureFileName` | `nvarchar(max)` | Nullable |
| `CatalogTypeId` | `int` | FK to `CatalogType.Id` |
| `CatalogBrandId` | `int` | FK to `CatalogBrand.Id` |
| `AvailableStock` | `int` | Required, default `0` |
| `RestockThreshold` | `int` | Required, default `0` |
| `MaxStockThreshold` | `int` | Required, default `0` |
| `OnReorder` | `bit` | Required, default `0` |

Sequences:

- `catalog_hilo`
- `catalogbrand_hilo`
- `catalogtype_hilo`

The API-computed `PictureUri` remains ignored by persistence and is constructed from service configuration and `PictureFileName`.

### 6.2 Initial Data

Load catalog data from the same source files used by .NET:

- `CatalogBrands.csv`
- `CatalogTypes.csv`
- `CatalogItems.csv`
- `CatalogItems.zip`

CSV header compatibility:

- Brands: `catalogbrand`
- Types: `catalogtype`
- Items: `catalogtypename,catalogbrandname,description,name,price,picturefilename,availablestock,restockthreshold,maxstockthreshold,onreorder`

The current source CSV uses `CatalogTypeName,CatalogBrandName,Description,Name,Price,PictureFileName,availablestock,onreorder`. The loader must accept missing optional `restockthreshold` and `maxstockthreshold` and default them to `0`.

Seed rules:

- Insert brands before types and items.
- Preserve generated ids based on insertion order because catalog item seed rows resolve brand/type ids by name.
- Use upsert-by-name for `CatalogBrand` and `CatalogType`.
- Use upsert-by-`Name` plus `PictureFileName` for `Catalog`.
- Extract `CatalogItems.zip` into the catalog static image directory during startup if the destination image is missing.
- Do not delete user-added catalog rows or custom images on restart.

## 7. Ordering Data Design

### 7.1 Schema

The ordering service owns schema `ordering` in `Microsoft.eShopOnContainers.Services.OrderingDb`.

`ordering.cardtypes`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, value generated never |
| `Name` | `nvarchar(200)` | Required |

`ordering.orderstatus`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, value generated never |
| `Name` | `nvarchar(200)` | Required |

`ordering.buyers`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `ordering.buyerseq` |
| `IdentityGuid` | `nvarchar(200)` | Required, unique |
| `Name` | `nvarchar(max)` | Nullable |

`ordering.paymentmethods`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `ordering.paymentseq` |
| `BuyerId` | `int` | Required, FK to `ordering.buyers.Id`, cascade delete |
| `CardTypeId` | `int` | Required, FK to `ordering.cardtypes.Id` |
| `Alias` | `nvarchar(200)` | Required |
| `CardNumber` | `nvarchar(25)` | Required |
| `SecurityNumber` | `nvarchar(max)` | Nullable if final EF migration omits it |
| `CardHolderName` | `nvarchar(200)` | Required |
| `Expiration` | `datetime2` | Required |

`ordering.orders`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `ordering.orderseq` |
| `BuyerId` | `int` | Nullable FK to `ordering.buyers.Id` |
| `OrderDate` | `datetime2` | Required |
| `OrderStatusId` | `int` | Required FK to `ordering.orderstatus.Id` |
| `PaymentMethodId` | `int` | Nullable FK to `ordering.paymentmethods.Id`, restrict delete |
| `Description` | `nvarchar(max)` | Nullable |
| `Address_Street` | `nvarchar(max)` | Required for submitted orders |
| `Address_City` | `nvarchar(max)` | Required for submitted orders |
| `Address_State` | `nvarchar(max)` | Required for submitted orders |
| `Address_Country` | `nvarchar(max)` | Required for submitted orders |
| `Address_ZipCode` | `nvarchar(max)` | Required for submitted orders |

`ordering.orderItems`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `orderitemseq` |
| `OrderId` | `int` | Required, FK to `ordering.orders.Id` |
| `ProductId` | `int` | Required |
| `ProductName` | `nvarchar(max)` | Required |
| `UnitPrice` | `decimal(18,2)` | Required |
| `Discount` | `decimal(18,2)` | Required |
| `Units` | `int` | Required |
| `PictureUrl` | `nvarchar(max)` | Nullable |

`ordering.requests`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `uniqueidentifier` | Primary key, matches `x-requestid` |
| `Name` | `nvarchar(max)` | Required |
| `Time` | `datetime2` | Required |

Sequences:

- `ordering.orderseq`
- `ordering.buyerseq`
- `ordering.paymentseq`
- `orderitemseq` as defined by the final EF migration state

### 7.2 Initial Data

Card types must preserve source enumeration ids:

| Id | Name |
| --- | --- |
| 1 | `Amex` |
| 2 | `Visa` |
| 3 | `MasterCard` |

Order statuses must preserve source enumeration ids:

| Id | Name |
| --- | --- |
| 1 | `submitted` |
| 2 | `awaitingvalidation` |
| 3 | `stockconfirmed` |
| 4 | `paid` |
| 5 | `shipped` |
| 6 | `cancelled` |

Seed rules:

- Use idempotent `MERGE` statements keyed by `Id`.
- Never reseed orders, buyers, payment methods, or requests.
- The `requests` table is runtime idempotency state and must be retained across restarts.
- The ordering API must continue to store incoming `x-requestid` values in `ordering.requests.Id`.

## 8. Integration Event Log Design

SQL-backed services that publish integration events need the same event outbox table used by the .NET app. Catalog and ordering require it immediately; other SQL services may include it if they publish events later.

`IntegrationEventLog`

| Column | Type | Rules |
| --- | --- | --- |
| `EventId` | `uniqueidentifier` | Primary key |
| `EventTypeName` | `nvarchar(max)` | Required |
| `State` | `int` | Required |
| `TimesSent` | `int` | Required |
| `CreationTime` | `datetime2` | Required |
| `Content` | `nvarchar(max)` | Required JSON |
| `TransactionId` | `nvarchar(max)` | Nullable |

State values must match the source enum semantics:

- `0` - not published
- `1` - in progress
- `2` - published
- `3` - published failed

Seed rules:

- No initial rows.
- Flyway repeatable script `R__integration_event_log.sql` may be copied into each publishing service database.
- Event `Content` JSON must preserve the existing integration event field names and casing.

## 9. Identity Data Design

### 9.1 User Profile Schema

The identity service must preserve the source user data model from ASP.NET Identity `ApplicationUser`.

Create a source-compatible user table, named `AspNetUsers` unless the implementation introduces a documented compatibility view with that name.

Required eShop custom fields:

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `nvarchar(450)` | Primary key |
| `UserName` | `nvarchar(256)` | Required |
| `NormalizedUserName` | `nvarchar(256)` | Required, unique |
| `Email` | `nvarchar(256)` | Required |
| `NormalizedEmail` | `nvarchar(256)` | Required |
| `EmailConfirmed` | `bit` | Required, default `0` |
| `PasswordHash` | `nvarchar(max)` | Required |
| `SecurityStamp` | `nvarchar(max)` | Required |
| `ConcurrencyStamp` | `nvarchar(max)` | Nullable |
| `PhoneNumber` | `nvarchar(max)` | Nullable |
| `PhoneNumberConfirmed` | `bit` | Required, default `0` |
| `TwoFactorEnabled` | `bit` | Required, default `0` |
| `LockoutEnd` | `datetimeoffset` | Nullable |
| `LockoutEnabled` | `bit` | Required, default `0` |
| `AccessFailedCount` | `int` | Required, default `0` |
| `CardNumber` | `nvarchar(max)` | Required |
| `SecurityNumber` | `nvarchar(max)` | Required |
| `Expiration` | `nvarchar(max)` | Required, `MM/YY` |
| `CardHolderName` | `nvarchar(max)` | Required |
| `CardType` | `int` | Required |
| `Street` | `nvarchar(max)` | Required |
| `City` | `nvarchar(max)` | Required |
| `State` | `nvarchar(max)` | Required |
| `Country` | `nvarchar(max)` | Required |
| `ZipCode` | `nvarchar(max)` | Required |
| `Name` | `nvarchar(max)` | Required |
| `LastName` | `nvarchar(max)` | Required |

The Java authentication layer may load users from this table through a custom `UserDetailsService`. Do not require the original ASP.NET password hash format at runtime; generate Spring Security compatible hashes during seed loading.

### 9.2 Spring Authorization Server Schema

Map IdentityServer configuration into Spring Authorization Server tables:

- `oauth2_registered_client`
- `oauth2_authorization`
- `oauth2_authorization_consent`

The registered client loader must preserve these source client ids:

- `js`
- `xamarin`
- `mvc`
- `webhooksclient`
- `mvctest`
- `locationsswaggerui`
- `marketingswaggerui`
- `basketswaggerui`
- `orderingswaggerui`
- `mobileshoppingaggswaggerui`
- `webshoppingaggswaggerui`
- `webhooksswaggerui`

Preserve these scopes/resources:

- `openid`
- `profile`
- `offline_access`
- `orders`
- `basket`
- `marketing`
- `locations`
- `mobileshoppingagg`
- `webshoppingagg`
- `orders.signalrhub`
- `webhooks`

Preserve source behavior:

- MVC and webhook client use confidential clients with secret `secret`.
- SPA and Swagger UI clients allow browser-based access.
- Mobile and MVC clients allow offline access where configured in source.
- MVC and webhook client token lifetime remains 2 hours.
- Redirect URIs, logout redirect URIs, and CORS origins are generated from the same environment properties used by the converted service configuration.

### 9.3 Initial Data

Load users from `Setup\Users.csv`.

CSV headers:

```text
CardHolderName,CardNumber,CardType,City,Country,Email,Expiration,LastName,Name,PhoneNumber,UserName,ZipCode,State,Street,SecurityNumber,NormalizedEmail,NormalizedUserName,Password
```

Default demo user:

- User name: `demouser@microsoft.com`
- Email: `demouser@microsoft.com`
- Password: `Pass@word1`
- Card number: `4012888888881881`
- Security number: `535`
- Address: `15703 NE 61st Ct`, Redmond, WA, `98052`, U.S.

Seed rules:

- Upsert users by `NormalizedUserName`.
- Generate new ids only for missing users.
- Hash plain CSV passwords with Spring Security's configured password encoder.
- Extract `Setup\images.zip` into the identity static image directory if the destination image exists in the expected image set and is missing or stale.
- Upsert registered clients by `client_id`.
- Do not delete authorization grants or consents on restart.

## 10. Marketing Data Design

### 10.1 SQL Schema

The marketing service owns `Microsoft.eShopOnContainers.Services.MarketingDb`.

`Campaign`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `campaign_hilo` |
| `Name` | `nvarchar(max)` | Required |
| `Description` | `nvarchar(max)` | Required |
| `From` | `datetime2` | Required |
| `To` | `datetime2` | Required |
| `PictureUri` | `nvarchar(max)` | Required |
| `PictureName` | `nvarchar(max)` | Nullable |
| `DetailsUri` | `nvarchar(max)` | Nullable, added by later migration |

`Rule`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, generated by `rule_hilo` |
| `CampaignId` | `int` | Required, FK to `Campaign.Id` |
| `Description` | `nvarchar(max)` | Required |
| `RuleTypeId` | `int` | Required discriminator |
| `LocationId` | `int` | Required for user-location rules |

Rule type ids:

- `1` - user profile rule
- `2` - purchase history rule
- `3` - user location rule

### 10.2 Initial Data

Seed the two source campaigns:

1. `.NET Bot Black Hoodie 50% OFF`
   - Description: `Campaign Description 1`
   - Picture name: `1.png`
   - Picture URI template: `http://externalcatalogbaseurltobereplaced/api/v1/campaigns/1/pic`
   - Rule: user location rule for `LocationId = 1`, description `Campaign is only for United States users.`

2. `Roslyn Red T-Shirt 3x2`
   - Description: `Campaign Description 2`
   - Picture name: `2.png`
   - Picture URI template: `http://externalcatalogbaseurltobereplaced/api/v1/campaigns/2/pic`
   - Rule: user location rule for `LocationId = 3`, description `Campaign is only for Seattle users.`

The source uses `DateTime.Now` for campaign ranges. The Java seed should use a deterministic relative seed strategy:

- Campaign 1: `from = current startup date`, `to = current startup date + 7 days`
- Campaign 2: `from = current startup date - 7 days`, `to = current startup date + 14 days`

Only apply those dates on initial insert. Do not rewrite campaign date ranges on restart.

### 10.3 Mongo Read Model

Mongo database: configured marketing Mongo database, matching source `MarketingSettings.MongoDatabase`.

Collection: `MarketingReadDataModel`

Document shape:

```json
{
  "_id": "ObjectId",
  "UserId": "string",
  "Locations": [
    {
      "LocationId": 4,
      "Code": "SEAT",
      "Description": "Seattle"
    }
  ],
  "UpdateDate": "ISO-8601 date"
}
```

Seed rules:

- No initial marketing read model documents.
- Documents are created from user location integration events.
- Create an index on `UserId` for lookup performance.

## 11. Locations Data Design

### 11.1 Mongo Collections

Mongo database: configured locations database, matching source `LocationSettings.Database`.

Collections:

- `Locations`
- `UserLocation`

`Locations` document shape:

```json
{
  "_id": "ObjectId",
  "LocationId": 4,
  "Code": "SEAT",
  "Parent_Id": "ObjectId",
  "Description": "Seattle",
  "Latitude": 47.603111,
  "Longitude": -122.330747,
  "Location": {
    "type": "Point",
    "coordinates": [-122.330747, 47.603111]
  },
  "Polygon": {
    "type": "Polygon",
    "coordinates": [[[-122.36238, 47.82929]]]
  }
}
```

`UserLocation` document shape:

```json
{
  "_id": "ObjectId",
  "UserId": "string",
  "LocationId": 4,
  "UpdateDate": "ISO-8601 date"
}
```

Indexes:

- `Locations.Location` as `2dsphere`
- `Locations.LocationId` unique
- `UserLocation.UserId` unique or non-unique based on final API behavior; prefer unique if the implementation updates one current location per user

### 11.2 Initial Data

Seed these location records with the same codes, descriptions, parent hierarchy, coordinates, and polygons as `LocationsContextSeed`.

| LocationId | Code | Description | Parent |
| --- | --- | --- | --- |
| 1 | `NA` | North America | none |
| 2 | `US` | United States | `NA` |
| 3 | `WHT` | Washington | `US` |
| 4 | `SEAT` | Seattle | `WHT` |
| 5 | `REDM` | Redmond | `WHT` |
| 6 | `BCN` | Barcelona | none |
| 7 | `SA` | South America | none |
| 8 | `AFC` | Africa | none |
| 9 | `EU` | Europe | none |
| 10 | `AS` | Asia | none |
| 11 | `AUS` | Australia | none |

Seed rules:

- Insert parent locations before children.
- Use `LocationId` as the idempotency key.
- Preserve Mongo `_id` references in `Parent_Id`; do not replace parent links with numeric ids.
- Load polygon coordinates from a checked-in JSON fixture generated from the .NET seed source.

## 12. Basket Data Design

Basket has no SQL schema. It stores one Redis string per buyer id.

Redis key:

```text
{buyerId}
```

Value shape:

```json
{
  "BuyerId": "string",
  "Items": [
    {
      "Id": "string",
      "ProductId": 1,
      "ProductName": ".NET Bot Black Hoodie",
      "UnitPrice": 19.5,
      "OldUnitPrice": 0,
      "Quantity": 1,
      "PictureUrl": "string"
    }
  ]
}
```

Compatibility rules:

- Read both .NET PascalCase JSON and Java camelCase JSON if existing Redis data is present.
- Write JSON in the public API casing expected by existing clients.
- Preserve item validation: `Quantity` must be at least `1`.
- No initial baskets are seeded.
- Checkout deletes or updates the basket using the same buyer id key semantics as .NET.

## 13. Webhooks Data Design

The webhooks service owns `Microsoft.eShopOnContainers.Services.WebhooksDb`.

`Subscriptions`

| Column | Type | Rules |
| --- | --- | --- |
| `Id` | `int` | Primary key, identity |
| `Type` | `int` | Required |
| `Date` | `datetime2` | Required |
| `DestUrl` | `nvarchar(max)` | Nullable |
| `Token` | `nvarchar(max)` | Nullable |
| `UserId` | `nvarchar(max)` | Nullable |

Webhook type ids:

| Id | Name |
| --- | --- |
| 1 | `CatalogItemPriceChange` |
| 2 | `OrderShipped` |
| 3 | `OrderPaid` |

Seed rules:

- No initial subscription rows.
- Subscriptions are runtime user data and must survive service restart.

## 14. Dependency Changes

Add dependencies during implementation as follows.

SQL-backed services:

- `org.springframework.boot:spring-boot-starter-data-jpa` or JDBC where JPA is not used
- `com.microsoft.sqlserver:mssql-jdbc`
- `org.flywaydb:flyway-core`
- SQL Server Flyway support artifact if required by the selected Flyway version

Mongo-backed services:

- `org.springframework.boot:spring-boot-starter-data-mongodb`

Redis-backed services:

- `org.springframework.boot:spring-boot-starter-data-redis`
- Jackson configuration that accepts both PascalCase and camelCase basket payloads

Identity:

- `org.springframework.boot:spring-boot-starter-security`
- Spring Authorization Server
- password encoder configuration

Tests:

- `org.testcontainers:junit-jupiter`
- `org.testcontainers:mssqlserver`
- `org.testcontainers:mongodb`
- `org.testcontainers:rabbitmq`
- Redis Testcontainers support through `GenericContainer`

## 15. Implementation Sequence

1. Add database dependencies and configuration properties to each persistent module.
2. Add Flyway migration scripts for catalog, ordering, identity, marketing, webhooks, and integration event log.
3. Add Java persistence entities or JDBC repositories matching the schemas above.
4. Add idempotent seed loaders for CSV, ZIP, Mongo JSON, identity registered clients, and password hashing.
5. Replace in-memory repositories/controllers with persistent repositories while preserving public API payloads.
6. Add Testcontainers integration tests for schema and seed validation.
7. Run the local app and validate API smoke flows against seeded data.

## 16. Migration Test Plan

### 16.1 SQL Server Tests

For each SQL-backed service, start SQL Server with Testcontainers and verify:

- Flyway applies all migrations from an empty database.
- Re-running Flyway and seed loaders is idempotent.
- Required tables, schemas, sequences, indexes, and constraints exist.
- Seed row counts and values match this document.
- Runtime tables are not cleared on restart.

Catalog assertions:

- `CatalogBrand`, `CatalogType`, and `Catalog` are populated from CSV.
- Catalog item stock fields match source values.
- Picture file names match source values.
- Image extraction makes the expected files available to the API.

Ordering assertions:

- `ordering.cardtypes` contains ids `1..3`.
- `ordering.orderstatus` contains ids `1..6`.
- `ordering.requests.Id` accepts the API `x-requestid` UUID.
- Order creation persists embedded address fields and order items.

Identity assertions:

- Demo user can authenticate with `Pass@word1`.
- Demo user profile fields match `Users.csv`.
- All source client ids and scopes exist in Spring Authorization Server registered clients.
- MVC, SPA, Swagger UI, mobile, webhook client, and aggregator redirect URIs match configured service URLs.

Marketing assertions:

- Two campaigns and two user-location rules exist.
- `DetailsUri` column exists.
- Campaign image names and picture URI templates match source.

Webhooks assertions:

- `Subscriptions` table exists with the exact source columns.
- Webhook type ids map to existing event handlers.

Integration event log assertions:

- `IntegrationEventLog` exists in publishing service databases.
- `TransactionId` exists and is nullable.
- Event state integer values serialize and deserialize correctly.

### 16.2 MongoDB Tests

Locations:

- `Locations` collection contains 11 seed documents.
- Parent-child links use Mongo object ids.
- `LocationId` values and codes match the source table in this document.
- `Location` has a 2dsphere index.
- Geospatial lookup for Seattle returns the Seattle or Washington hierarchy as expected by the API.

Marketing read model:

- `MarketingReadDataModel` collection exists.
- `UserId` index exists.
- User location update events create or update one read model document for the user.

### 16.3 Redis Tests

- Empty Redis returns no basket for a missing buyer.
- Updating a basket writes a string value keyed by buyer id.
- PascalCase .NET JSON can be read by the Java service.
- Java-written JSON can be read back and returned through the public API shape.
- Invalid quantity below `1` is rejected.

### 16.4 End-to-End Smoke Tests

Run the composed Spring services and verify:

1. Browse catalog from seeded SQL data.
2. Add and update basket items in Redis.
3. Checkout with `x-requestid`; verify request idempotency in `ordering.requests`.
4. Create an order and persist order items/address/payment references.
5. Run order lifecycle transitions through stock confirmation, payment, shipping, and cancellation.
6. Retrieve campaigns filtered by seeded location data.
7. Register a webhook subscription and receive a dispatched webhook event.
8. Authenticate demo user through the converted identity service.
9. Confirm `/actuator/health` or mapped health endpoints report SQL, MongoDB, Redis, and RabbitMQ readiness.

## 17. Acceptance Criteria

- A clean environment can start with empty SQL Server databases, empty Mongo databases, empty Redis, and RabbitMQ, then reach a usable seeded state automatically.
- All seeded API responses match the .NET app's public contract for ids, JSON field names, enum values, pagination shape, and image file names.
- Re-running services does not duplicate seed data or erase runtime data.
- Original Compose database boundaries and service ownership are preserved.
- The Spring app can authenticate the demo user and issue tokens for the same clients/scopes used by the .NET app.
- Contract and integration tests cover SQL schema, Mongo collections, Redis basket JSON, identity clients/users, and public API smoke flows.

