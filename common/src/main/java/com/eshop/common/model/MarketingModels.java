package com.eshop.common.model;

import java.time.OffsetDateTime;
import java.util.List;

public final class MarketingModels {
    private MarketingModels() {
    }

    public record CampaignDTO(Integer id, String name, String description, OffsetDateTime from, OffsetDateTime to,
                              String pictureUri, String detailsUri) {
    }

    public record UserLocationRuleDTO(Integer id, String description, Integer locationId) {
    }

    public record UserLocationDetails(Integer locationId, String name) {
    }

    public record UserLocationDTO(String userId, List<UserLocationDetails> locations) {
    }
}
