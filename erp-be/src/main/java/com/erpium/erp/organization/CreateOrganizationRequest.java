package com.erpium.erp.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Immutable request DTO for creating an Organization.
 * Uses Java record and Bean Validation per backend-api.md.
 */
public record CreateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Organization code is required")
        @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code must contain only alphanumeric characters, underscores or hyphens")
        String code,

        @Size(max = 30, message = "Tax code must not exceed 30 characters")
        String taxCode
) {
}
