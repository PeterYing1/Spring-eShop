package com.eshop.common.model;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogModels {
    private CatalogModels() {
    }

    public record CatalogBrand(Integer id, String brand) {
    }

    public record CatalogType(Integer id, String type) {
    }

    public record CatalogItem(
            Integer id,
            String name,
            String description,
            BigDecimal price,
            String pictureFileName,
            String pictureUri,
            Integer catalogTypeId,
            CatalogType catalogType,
            Integer catalogBrandId,
            CatalogBrand catalogBrand,
            Integer availableStock,
            Integer restockThreshold,
            Integer maxStockThreshold,
            Boolean onReorder) {
    }

    public record PaginatedItems<T>(int pageIndex, int pageSize, long count, List<T> data) {
    }
}
