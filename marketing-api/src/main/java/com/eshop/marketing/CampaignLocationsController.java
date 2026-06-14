package com.eshop.marketing;

import com.eshop.common.model.MarketingModels.UserLocationRuleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class CampaignLocationsController {
    private final AtomicInteger ids = new AtomicInteger();
    private final Map<Integer, List<UserLocationRuleDTO>> rulesByCampaign = new ConcurrentHashMap<>();

    @GetMapping("/api/v1/campaigns/{campaignId}/locations/{userLocationRuleId}")
    public ResponseEntity<UserLocationRuleDTO> byId(@PathVariable int campaignId, @PathVariable int userLocationRuleId) {
        return rulesByCampaign.getOrDefault(campaignId, List.of()).stream()
                .filter(r -> r.id() == userLocationRuleId)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/campaigns/{campaignId}/locations")
    public List<UserLocationRuleDTO> all(@PathVariable int campaignId) {
        return rulesByCampaign.getOrDefault(campaignId, List.of());
    }

    @PostMapping("/api/v1/campaigns/{campaignId}/locations")
    public ResponseEntity<Void> create(@PathVariable int campaignId, @RequestBody UserLocationRuleDTO rule) {
        int id = ids.incrementAndGet();
        rulesByCampaign.computeIfAbsent(campaignId, ignored -> new ArrayList<>())
                .add(new UserLocationRuleDTO(id, rule.description(), rule.locationId()));
        return ResponseEntity.created(URI.create("/api/v1/campaigns/" + campaignId + "/locations/" + id)).build();
    }

    @DeleteMapping("/api/v1/campaigns/{campaignId}/locations/{userLocationRuleId}")
    public ResponseEntity<Void> delete(@PathVariable int campaignId, @PathVariable int userLocationRuleId) {
        List<UserLocationRuleDTO> rules = rulesByCampaign.get(campaignId);
        boolean removed = rules != null && rules.removeIf(r -> r.id() == userLocationRuleId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
