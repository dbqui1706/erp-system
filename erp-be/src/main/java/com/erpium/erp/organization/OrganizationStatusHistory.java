package com.erpium.erp.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable audit entity recording Organization lifecycle state transitions.
 * Satisfies regulatory audit trail requirements (who, what, when, why).
 *
 * Architecture Compliance:
 * - backend.md: Financial and operational audit trails are append-only.
 * - backend-jpa.md: Explicit column constraints; UTC Instant timestamps; immutable records.
 */
@Entity
@Table(name = "organization_status_histories")
public class OrganizationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private OrganizationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrganizationStatus toStatus;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OrganizationStatusHistory() {
        // Required by JPA specification
    }

    public OrganizationStatusHistory(Long organizationId, OrganizationStatus fromStatus, OrganizationStatus toStatus, String reason) {
        this.organizationId = organizationId;
        this.fromStatus = Objects.requireNonNull(fromStatus, "fromStatus must not be null");
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus must not be null");
        this.reason = (reason == null || reason.isBlank()) ? "Standard lifecycle transition" : reason.trim();
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public OrganizationStatus getFromStatus() { return fromStatus; }
    public OrganizationStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
