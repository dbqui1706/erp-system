package com.erpium.erp.product;

import java.math.BigDecimal;

/**
 * Calculated response DTO returned by the Dynamic Price Engine.
 * Solves the Static Pricing Illusion by detailing base price, effective tier price,
 * VAT gross unit price, and total line amount.
 */
public record EffectivePriceResponse(
        Long productId,
        String sku,
        String priceListCode,
        Integer requestedQuantity,
        BigDecimal unitBasePrice,
        BigDecimal grossUnitPrice,
        BigDecimal totalGrossAmount,
        String currency,
        String appliedRule
) {
}
