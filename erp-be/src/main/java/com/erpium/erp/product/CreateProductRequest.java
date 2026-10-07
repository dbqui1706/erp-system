package com.erpium.erp.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Immutable request DTO for creating a new product.
 * Uses Java 21 record and Jakarta Bean Validation per backend-api.md.
 */
public record CreateProductRequest(
        @NotNull(message = "Organization ID is required")
        Long organizationId,

        @NotBlank(message = "SKU is required")
        @Size(min = 2, max = 50, message = "SKU must be between 2 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "SKU must contain only alphanumeric characters, underscores or hyphens")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must not exceed 150 characters")
        String name,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Base price cannot be negative")
        BigDecimal basePrice,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. USD, VND)")
        String currency,

        @NotNull(message = "Tax rate is required")
        @DecimalMin(value = "0.0", message = "Tax rate cannot be negative")
        @DecimalMax(value = "1.0", message = "Tax rate cannot exceed 1.0 (100%)")
        BigDecimal taxRate
) {
}
