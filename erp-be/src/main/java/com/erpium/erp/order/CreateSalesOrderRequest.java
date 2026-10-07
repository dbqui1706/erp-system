package com.erpium.erp.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for creating a new Sales Order with initial line items.
 */
public record CreateSalesOrderRequest(
        @NotNull(message = "Organization ID is required")
        Long organizationId,

        @NotBlank(message = "Order number is required")
        @Size(min = 3, max = 50, message = "Order number must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Order number must contain only alphanumeric characters, underscores or hyphens")
        String orderNumber,

        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Customer name must not exceed 150 characters")
        String customerName,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotEmpty(message = "Order must contain at least one line item")
        List<@Valid CreateOrderLineItemRequest> lines
) {
}
