package com.erpium.erp.organization;

import java.time.Instant;

/**
 * Immutable response DTO representing an audit trail entry of status changes.
 */
public record OrganizationStatusHistoryResponse(
        Long id,
        Long organizationId,
        OrganizationStatus fromStatus,
        OrganizationStatus toStatus,
        String reason,
        Instant createdAt
) {
    public static OrganizationStatusHistoryResponse fromEntity(OrganizationStatusHistory entity) {
        return new OrganizationStatusHistoryResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
