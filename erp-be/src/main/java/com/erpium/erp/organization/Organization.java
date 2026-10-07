package com.erpium.erp.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

/**
 * Organization domain entity representing an enterprise legal entity or operational tenant.
 *
 * Architecture Compliance:
 * - backend.md: Domain logic encapsulated inside entity; optimistic concurrency via @Version.
 * - backend-jpa.md: Explicit schema column mappings; UTC Instant timestamps; business key equals/hashCode.
 */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    /**
     * Replaced naive boolean with explicit State Machine.
     * Persisted as STRING to avoid ordinal rearrangement risks in database migrations.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationStatus status = OrganizationStatus.DRAFT;

    /**
     * Optimistic locking version preventing concurrent overwrite anomalies (Lost Updates).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /**
     * Protected no-args constructor required by the JPA specification.
     */
    protected Organization() {
    }

    /**
     * Primary domain constructor. Organizations always start in DRAFT status.
     */
    public Organization(String name, String code, String taxCode) {
        this.name = Objects.requireNonNull(name, "Organization name must not be null");
        this.code = Objects.requireNonNull(code, "Organization code must not be null").toUpperCase();
        this.taxCode = taxCode;
        this.status = OrganizationStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Package-private constructor for testing and hydration with pre-set ID.
     */
    Organization(Long id, String name, String code, String taxCode) {
        this(name, code, taxCode);
        this.id = id;
    }

    // =========================================================================
    // STATE MACHINE TRANSITIONS (Business Invariants)
    // =========================================================================

    /**
     * Activates the organization. Allowed only from DRAFT or SUSPENDED states.
     *
     * @throws IllegalStateException if organization is CLOSED or already ACTIVE.
     */
    public void activate() {
        if (this.status == OrganizationStatus.CLOSED) {
            throw new IllegalStateException("Cannot activate an organization that is permanently CLOSED.");
        }
        if (this.status == OrganizationStatus.ACTIVE) {
            throw new IllegalStateException("Organization is already ACTIVE.");
        }
        this.status = OrganizationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Suspends an active organization. Allowed only from ACTIVE state.
     *
     * @throws IllegalStateException if organization is not ACTIVE.
     */
    public void suspend() {
        if (this.status != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE organizations can be suspended. Current status: " + this.status);
        }
        this.status = OrganizationStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    /**
     * Permanently closes an organization. Allowed from ACTIVE or SUSPENDED.
     * Once closed, an organization can never transition to any other status.
     *
     * @throws IllegalStateException if organization is already CLOSED or in DRAFT.
     */
    public void close() {
        if (this.status == OrganizationStatus.CLOSED) {
            throw new IllegalStateException("Organization is already CLOSED.");
        }
        if (this.status == OrganizationStatus.DRAFT) {
            throw new IllegalStateException("DRAFT organizations cannot be closed directly; delete or activate first.");
        }
        this.status = OrganizationStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Updates mutable organization details. Prohibited if organization is CLOSED.
     */
    public void updateDetails(String newName, String newTaxCode) {
        if (this.status == OrganizationStatus.CLOSED) {
            throw new IllegalStateException("Cannot modify details of a permanently CLOSED organization.");
        }
        this.name = Objects.requireNonNull(newName, "Organization name must not be null");
        this.taxCode = newTaxCode;
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // ACCESSORS
    // =========================================================================

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getTaxCode() { return taxCode; }
    public OrganizationStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization that)) return false;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
