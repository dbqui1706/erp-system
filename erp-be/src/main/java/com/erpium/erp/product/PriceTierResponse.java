package com.erpium.erp.product;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable response DTO representing a configured price tier.
 */
public record PriceTierResponse(
        Long id,
        Long productId,
        String priceListCode,
        Integer minQuantity,
        BigDecimal tierPrice,
        Instant createdAt
) {
    public static PriceTierResponse fromEntity(ProductPriceTier entity) {
        return new PriceTierResponse(
                entity.getId(),
                entity.getProductId(),
                entity.getPriceListCode(),
                entity.getMinQuantity(),
                entity.getTierPrice(),
                entity.getCreatedAt()
        );
    }
}
