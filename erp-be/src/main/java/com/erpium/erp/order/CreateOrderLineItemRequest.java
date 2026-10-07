package com.erpium.erp.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for adding a line item to a Sales Order.
 */
public record CreateOrderLineItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @Size(max = 50, message = "Price list code must not exceed 50 characters")
        String priceListCode
) {
}
