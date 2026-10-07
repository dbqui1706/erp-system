package com.erpium.erp.organization;

import java.time.Instant;

/**
 * Immutable response DTO representing an Organization.
 * Decouples API consumers from internal persistence entities.
 *
 * Fully compatible with MapStruct compile-time generation on Java 21 records.
 */
public record OrganizationResponse(
        Long id,
        String name,
        String code,
        String taxCode,
        OrganizationStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
