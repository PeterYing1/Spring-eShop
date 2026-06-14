package com.eshop.common.model;

import java.util.List;

public final class LocationModels {
    private LocationModels() {
    }

    public record LocationPoint(double latitude, double longitude) {
    }

    public record LocationPolygon(List<LocationPoint> points) {
    }

    public record Locations(Integer locationId, String code, String description, LocationPolygon polygon) {
    }

    public record UserLocationDetails(Integer locationId, String description) {
    }

    public record UserLocation(String userId, List<UserLocationDetails> locations) {
    }

    public record LocationRequest(List<UserLocationDetails> locations) {
    }
}
