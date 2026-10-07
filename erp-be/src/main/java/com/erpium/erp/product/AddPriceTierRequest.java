package com.erpium.erp.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for configuring a price tier for a product.
 */
public record AddPriceTierRequest(
        @NotBlank(message = "Price list code is required")
        @Size(max = 50, message = "Price list code must not exceed 50 characters")
        String priceListCode,

        @NotNull(message = "Minimum quantity is required")
        @Min(value = 1, message = "Minimum quantity must be at least 1")
        Integer minQuantity,

        @NotNull(message = "Tier price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Tier price cannot be negative")
        BigDecimal tierPrice
) {
}
