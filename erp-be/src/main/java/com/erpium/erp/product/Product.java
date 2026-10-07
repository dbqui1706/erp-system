package com.erpium.erp.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Product domain entity representing a commercial catalog item.
 *
 * Architecture Compliance:
 * - backend.md: Modular Monolith boundary; references Organization via organizationId (no ORM entity coupling).
 * - backend.md: Monetary precision using BigDecimal (scale = 4 internal, scale = 2 presentation with HALF_UP).
 * - backend-jpa.md: Optimistic concurrency via @Version; explicit schema mapping; UTC Instant auditing.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Modular Monolith Loose Coupling:
     * Stores organization ID reference instead of direct @ManyToOne entity relationship
     * to keep persistence models completely isolated between modules.
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Internal base price before tax stored with high precision (4 decimal places)
     * to avoid compounding rounding errors during multi-quantity order calculations.
     */
    @Column(name = "base_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal basePrice;

    /**
     * ISO 4217 Currency Code (e.g., "USD", "VND", "EUR").
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * Applicable tax rate (e.g. 0.1000 for 10% VAT).
     */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Product() {
        // Required by JPA specification
    }

    public Product(Long organizationId, String sku, String name, BigDecimal basePrice, String currency, BigDecimal taxRate) {
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID must not be null");
        this.sku = Objects.requireNonNull(sku, "SKU must not be null").trim().toUpperCase();
        this.name = Objects.requireNonNull(name, "Product name must not be null").trim();
        this.basePrice = validatePrice(basePrice);
        this.currency = Objects.requireNonNull(currency, "Currency must not be null").trim().toUpperCase();
        this.taxRate = validateTaxRate(taxRate);
        this.status = ProductStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // DOMAIN PRICING CALCULATIONS
    // =========================================================================

    /**
     * Calculates the gross price after tax, rounded to standard commercial 2 decimal places using HALF_UP.
     * Formula: GrossPrice = BasePrice * (1 + TaxRate), rounded to scale 2.
     *
     * @return display gross price with scale = 2
     */
    public BigDecimal calculateGrossPrice() {
        BigDecimal multiplier = BigDecimal.ONE.add(this.taxRate);
        return this.basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Updates product pricing with validation and timestamp refresh.
     */
    public void updatePricing(BigDecimal newBasePrice, BigDecimal newTaxRate) {
        if (this.status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("Cannot update pricing for a DISCONTINUED product.");
        }
        this.basePrice = validatePrice(newBasePrice);
        this.taxRate = validateTaxRate(newTaxRate);
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // STATE MACHINE TRANSITIONS
    // =========================================================================

    public void activate() {
        if (this.status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("Cannot reactivate a DISCONTINUED product.");
        }
        this.status = ProductStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // VALIDATION HELPERS
    // =========================================================================

    private BigDecimal validatePrice(BigDecimal price) {
        Objects.requireNonNull(price, "Base price must not be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative: " + price);
        }
        return price.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal validateTaxRate(BigDecimal rate) {
        Objects.requireNonNull(rate, "Tax rate must not be null");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0.0 and 1.0 (got " + rate + ")");
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // ACCESSORS
    // =========================================================================

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getCurrency() { return currency; }
    public BigDecimal getTaxRate() { return taxRate; }
    public ProductStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sku);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", basePrice=" + basePrice +
                " " + currency +
                ", status=" + status +
                '}';
    }
}
