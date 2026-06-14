package com.eshop.locations;

import com.eshop.common.model.LocationModels.LocationPoint;
import com.eshop.common.model.LocationModels.LocationPolygon;
import com.eshop.common.model.LocationModels.LocationRequest;
import com.eshop.common.model.LocationModels.Locations;
import com.eshop.common.model.LocationModels.UserLocation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationsController {
    private final List<Locations> locations = List.of(new Locations(1, "SEA", "Seattle",
            new LocationPolygon(List.of(new LocationPoint(47.6, -122.3)))));
    private final Map<String, UserLocation> users = new ConcurrentHashMap<>();

    @GetMapping("/user/{userId}")
    public UserLocation user(@PathVariable String userId) {
        return users.getOrDefault(userId, new UserLocation(userId, List.of()));
    }

    @GetMapping
    public List<Locations> all() {
        return locations;
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<Locations> byId(@PathVariable int locationId) {
        return locations.stream().filter(l -> l.locationId() == locationId).findFirst()
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createOrUpdate(@RequestBody LocationRequest request) {
        users.put("anonymous", new UserLocation("anonymous", request.locations()));
        return ResponseEntity.ok().build();
    }
}
