package com.erpium.erp.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Immutable response DTO representing a full Sales Order with line items.
 */
public record SalesOrderResponse(
        Long id,
        Long organizationId,
        String orderNumber,
        String customerName,
        String currency,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<SalesOrderLineResponse> lines,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
