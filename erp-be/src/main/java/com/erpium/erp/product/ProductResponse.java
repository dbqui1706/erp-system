package com.erpium.erp.product;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable response DTO representing a Product catalog item.
 * Includes both internal base price (scale 4) and commercial gross price (scale 2).
 *
 * Generated at compile-time by MapStruct for Java 21 records.
 */
public record ProductResponse(
        Long id,
        Long organizationId,
        String sku,
        String name,
        BigDecimal basePrice,
        String currency,
        BigDecimal taxRate,
        BigDecimal grossPrice,
        ProductStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
