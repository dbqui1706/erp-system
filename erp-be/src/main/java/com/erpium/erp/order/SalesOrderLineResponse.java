package com.erpium.erp.order;

import java.math.BigDecimal;

/**
 * Immutable response DTO representing an order line item.
 */
public record SalesOrderLineResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineAmount,
        BigDecimal lineTaxAmount
) {
}
