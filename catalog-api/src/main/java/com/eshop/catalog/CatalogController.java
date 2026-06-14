package com.eshop.catalog;

import com.eshop.common.model.CatalogModels.CatalogBrand;
import com.eshop.common.model.CatalogModels.CatalogItem;
import com.eshop.common.model.CatalogModels.CatalogType;
import com.eshop.common.model.CatalogModels.PaginatedItems;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final AtomicInteger ids = new AtomicInteger();
    private final List<CatalogBrand> brands;
    private final List<CatalogType> types;
    private final Map<Integer, CatalogItem> items = new ConcurrentHashMap<>();

    public CatalogController() {
        Map<String, CatalogBrand> brandLookup = loadBrands();
        Map<String, CatalogType> typeLookup = loadTypes();
        this.brands = List.copyOf(brandLookup.values());
        this.types = List.copyOf(typeLookup.values());
        loadItems(typeLookup, brandLookup);
    }

    @GetMapping("/items")
    public ResponseEntity<?> items(@RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(defaultValue = "0") int pageIndex,
                                   @RequestParam(required = false) String ids) {
        if (ids != null && !ids.isBlank()) {
            List<CatalogItem> selected = parseIds(ids).stream().map(items::get).filter(i -> i != null).toList();
            return selected.isEmpty()
                    ? ResponseEntity.badRequest().body("ids value invalid. Must be comma-separated list of numbers")
                    : ResponseEntity.ok(selected);
        }
        return ResponseEntity.ok(page(allItems(), pageSize, pageIndex));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<CatalogItem> itemById(@PathVariable int id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        CatalogItem item = items.get(id);
        return item == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(item);
    }

    @GetMapping("/items/withname/{name}")
    public PaginatedItems<CatalogItem> itemsWithName(@PathVariable String name,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(defaultValue = "0") int pageIndex) {
        return page(allItems().stream().filter(i -> i.name().startsWith(name)).toList(), pageSize, pageIndex);
    }

    @GetMapping({"/items/type/{catalogTypeId}/brand", "/items/type/{catalogTypeId}/brand/{catalogBrandId}"})
    public PaginatedItems<CatalogItem> itemsByTypeAndBrand(@PathVariable int catalogTypeId,
                                                           @PathVariable(required = false) Integer catalogBrandId,
                                                           @RequestParam(defaultValue = "10") int pageSize,
                                                           @RequestParam(defaultValue = "0") int pageIndex) {
        return page(allItems().stream()
                .filter(i -> i.catalogTypeId() == catalogTypeId)
                .filter(i -> catalogBrandId == null || i.catalogBrandId().equals(catalogBrandId))
                .toList(), pageSize, pageIndex);
    }

    @GetMapping({"/items/type/all/brand", "/items/type/all/brand/{catalogBrandId}"})
    public PaginatedItems<CatalogItem> itemsByBrand(@PathVariable(required = false) Integer catalogBrandId,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(defaultValue = "0") int pageIndex) {
        return page(allItems().stream()
                .filter(i -> catalogBrandId == null || i.catalogBrandId().equals(catalogBrandId))
                .toList(), pageSize, pageIndex);
    }

    @GetMapping("/catalogtypes")
    public List<CatalogType> catalogTypes() {
        return types;
    }

    @GetMapping("/catalogbrands")
    public List<CatalogBrand> catalogBrands() {
        return brands;
    }

    @PutMapping("/items")
    public ResponseEntity<Void> update(@RequestBody CatalogItem item) {
        if (!items.containsKey(item.id())) {
            return ResponseEntity.notFound().build();
        }
        items.put(item.id(), item);
        return ResponseEntity.created(URI.create("/api/v1/catalog/items/" + item.id())).build();
    }

    @PostMapping("/items")
    public ResponseEntity<Void> create(@RequestBody CatalogItem item) {
        int id = ids.incrementAndGet();
        CatalogItem created = new CatalogItem(id, item.name(), item.description(), item.price(), item.pictureFileName(),
                "/api/v1/catalog/items/" + id + "/pic", item.catalogTypeId(), item.catalogType(), item.catalogBrandId(),
                item.catalogBrand(), item.availableStock(), item.restockThreshold(), item.maxStockThreshold(), item.onReorder());
        items.put(id, created);
        return ResponseEntity.created(URI.create("/api/v1/catalog/items/" + id)).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        return items.remove(id) == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private void addSeed(CatalogItem item) {
        items.put(item.id(), item);
        ids.updateAndGet(current -> Math.max(current, item.id()));
    }

    private List<CatalogItem> allItems() {
        return items.values().stream().sorted(Comparator.comparing(CatalogItem::name)).toList();
    }

    private PaginatedItems<CatalogItem> page(List<CatalogItem> source, int pageSize, int pageIndex) {
        int skip = Math.max(0, pageSize * pageIndex);
        List<CatalogItem> page = source.stream().skip(skip).limit(pageSize).toList();
        return new PaginatedItems<>(pageIndex, pageSize, source.size(), page);
    }

    private List<Integer> parseIds(String ids) {
        List<Integer> parsed = new ArrayList<>();
        for (String value : ids.split(",")) {
            try {
                parsed.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException ex) {
                return List.of();
            }
        }
        return parsed;
    }

    private Map<String, CatalogBrand> loadBrands() {
        Map<String, CatalogBrand> loaded = new LinkedHashMap<>();
        int id = 1;
        for (String line : readSetupLines("setup/CatalogBrands.csv")) {
            String brand = line.trim();
            if (!brand.isBlank() && !"CatalogBrand".equalsIgnoreCase(brand)) {
                loaded.put(brand, new CatalogBrand(id++, brand));
            }
        }
        return loaded.isEmpty()
                ? new LinkedHashMap<>(Map.of("Azure", new CatalogBrand(1, "Azure"), ".NET", new CatalogBrand(2, ".NET")))
                : loaded;
    }

    private Map<String, CatalogType> loadTypes() {
        Map<String, CatalogType> loaded = new LinkedHashMap<>();
        int id = 1;
        for (String line : readSetupLines("setup/CatalogTypes.csv")) {
            String type = line.trim();
            if (!type.isBlank() && !"CatalogType".equalsIgnoreCase(type)) {
                loaded.put(type, new CatalogType(id++, type));
            }
        }
        return loaded.isEmpty()
                ? new LinkedHashMap<>(Map.of("Mug", new CatalogType(1, "Mug"), "T-Shirt", new CatalogType(2, "T-Shirt")))
                : loaded;
    }

    private void loadItems(Map<String, CatalogType> typeLookup, Map<String, CatalogBrand> brandLookup) {
        int id = 1;
        for (String line : readSetupLines("setup/CatalogItems.csv")) {
            if (line.startsWith("CatalogTypeName,")) {
                continue;
            }
            List<String> columns = parseCsv(line);
            if (columns.size() < 8) {
                continue;
            }
            String typeName = columns.get(0).trim();
            String brandName = columns.get(1).trim();
            CatalogType type = typeLookup.get(typeName);
            CatalogBrand brand = brandLookup.get(brandName);
            if (type == null || brand == null) {
                continue;
            }
            int itemId = id++;
            String pictureFileName = columns.get(5).trim();
            addSeed(new CatalogItem(
                    itemId,
                    columns.get(3).trim(),
                    columns.get(2).trim(),
                    new BigDecimal(columns.get(4).trim()),
                    pictureFileName,
                    "/api/v1/catalog/items/" + itemId + "/pic",
                    type.id(),
                    type,
                    brand.id(),
                    brand,
                    parseInt(columns.get(6), 0),
                    0,
                    0,
                    Boolean.parseBoolean(columns.get(7).trim())));
        }
    }

    private List<String> readSetupLines(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return List.of();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> parseCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
