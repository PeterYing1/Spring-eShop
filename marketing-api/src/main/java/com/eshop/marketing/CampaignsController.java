package com.eshop.marketing;

import com.eshop.common.model.CatalogModels.PaginatedItems;
import com.eshop.common.model.MarketingModels.CampaignDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignsController {
    private final AtomicInteger ids = new AtomicInteger(2);
    private final Map<Integer, CampaignDTO> campaigns = new ConcurrentHashMap<>();

    public CampaignsController() {
        campaigns.put(1, new CampaignDTO(1, "New season", "Recommended products near you",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30),
                "/api/v1/campaigns/1/pic", null));
    }

    @GetMapping
    public List<CampaignDTO> all() {
        return campaigns.values().stream().toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDTO> byId(@PathVariable int id) {
        CampaignDTO campaign = campaigns.get(id);
        return campaign == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(campaign);
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CampaignDTO campaign) {
        int id = ids.incrementAndGet();
        campaigns.put(id, new CampaignDTO(id, campaign.name(), campaign.description(), campaign.from(), campaign.to(), campaign.pictureUri(), campaign.detailsUri()));
        return ResponseEntity.created(URI.create("/api/v1/campaigns/" + id)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable int id, @RequestBody CampaignDTO campaign) {
        if (!campaigns.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        campaigns.put(id, new CampaignDTO(id, campaign.name(), campaign.description(), campaign.from(), campaign.to(), campaign.pictureUri(), campaign.detailsUri()));
        return ResponseEntity.created(URI.create("/api/v1/campaigns/" + id)).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        return campaigns.remove(id) == null ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    public PaginatedItems<CampaignDTO> userCampaigns(@RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(defaultValue = "0") int pageIndex) {
        List<CampaignDTO> data = all().stream().skip((long) pageSize * pageIndex).limit(pageSize).toList();
        return new PaginatedItems<>(pageIndex, pageSize, campaigns.size(), data);
    }
}
