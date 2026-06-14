package com.eshop.locations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
final class LocationsSeedLoader implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final Resource seedResource;
    private final String locationsCollection;
    private final String userLocationCollection;
    private final Environment environment;

    LocationsSeedLoader(
            MongoTemplate mongoTemplate,
            ObjectMapper objectMapper,
            @Value("${eshop.locations.seed.resource}") Resource seedResource,
            @Value("${eshop.locations.mongo.locations-collection:Locations}") String locationsCollection,
            @Value("${eshop.locations.mongo.user-location-collection:UserLocation}") String userLocationCollection,
            Environment environment) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.seedResource = seedResource;
        this.locationsCollection = locationsCollection;
        this.userLocationCollection = userLocationCollection;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!shouldSeed()) {
            return;
        }

        mongoTemplate.indexOps(locationsCollection).ensureIndex(new GeospatialIndex("Location"));
        mongoTemplate.indexOps(locationsCollection).ensureIndex(new Index("LocationId", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps(userLocationCollection).ensureIndex(new Index("UserId", Sort.Direction.ASC));

        if (mongoTemplate.getCollection(locationsCollection).countDocuments() > 0) {
            return;
        }

        try (InputStream input = seedResource.getInputStream()) {
            List<JsonNode> seeds = objectMapper.readValue(input, new TypeReference<>() {
            });
            Map<String, Object> idsByCode = new LinkedHashMap<>();

            for (JsonNode seed : seeds) {
                Document document = toDocument(seed, idsByCode);
                mongoTemplate.getCollection(locationsCollection).insertOne(document);
                idsByCode.put(seed.path("Code").asText(), document.getObjectId("_id"));
            }
        }
    }

    private boolean shouldSeed() {
        String mode = environment.getProperty("eshop.locations.seed.enabled", "auto");
        if ("false".equalsIgnoreCase(mode)) {
            return false;
        }
        if ("true".equalsIgnoreCase(mode)) {
            return true;
        }
        String mongoUri = environment.getProperty("spring.data.mongodb.uri");
        return mongoUri != null && !mongoUri.isBlank();
    }

    private Document toDocument(JsonNode seed, Map<String, Object> idsByCode) {
        String parentCode = seed.path("ParentCode").isNull() ? null : seed.path("ParentCode").asText(null);
        double longitude = seed.path("Longitude").asDouble();
        double latitude = seed.path("Latitude").asDouble();

        Document document = new Document()
                .append("LocationId", seed.path("LocationId").asInt())
                .append("Code", seed.path("Code").asText())
                .append("Description", seed.path("Description").asText())
                .append("Latitude", latitude)
                .append("Longitude", longitude)
                .append("Location", new Document("type", "Point").append("coordinates", List.of(longitude, latitude)))
                .append("Polygon", new Document("type", "Polygon").append("coordinates", List.of(coordinates(seed.path("Polygon")))))
                .append("SeededAt", Instant.now().toString());

        if (parentCode != null && idsByCode.containsKey(parentCode)) {
            document.append("Parent_Id", idsByCode.get(parentCode));
        }

        return document;
    }

    private List<List<Double>> coordinates(JsonNode polygon) {
        List<List<Double>> coordinates = new ArrayList<>();
        for (JsonNode point : polygon) {
            coordinates.add(List.of(point.get(0).asDouble(), point.get(1).asDouble()));
        }
        return coordinates;
    }
}
