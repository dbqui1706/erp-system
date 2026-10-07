package com.erpium.erp.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a tiered price entry for a Product within a specific Price List.
 * Solves the Static Pricing Illusion (Bẫy đơn giá tĩnh) in B2B/Enterprise ERP.
 *
 * Example:
 * - Product 101, PriceList "WHOLESALE", minQuantity = 1,  tierPrice = $1100.0000
 * - Product 101, PriceList "WHOLESALE", minQuantity = 10, tierPrice = $1050.0000
 * - Product 101, PriceList "WHOLESALE", minQuantity = 50, tierPrice = $980.0000
 */
@Entity
@Table(name = "product_price_tiers")
public class ProductPriceTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "price_list_code", nullable = false, length = 50)
    private String priceListCode;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    @Column(name = "tier_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal tierPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ProductPriceTier() {
        // Required by JPA specification
    }

    public ProductPriceTier(Long productId, String priceListCode, Integer minQuantity, BigDecimal tierPrice) {
        this.productId = Objects.requireNonNull(productId, "Product ID must not be null");
        this.priceListCode = Objects.requireNonNull(priceListCode, "Price list code must not be null").trim().toUpperCase();
        if (minQuantity == null || minQuantity < 1) {
            throw new IllegalArgumentException("Minimum quantity must be at least 1 (got " + minQuantity + ")");
        }
        this.minQuantity = minQuantity;
        Objects.requireNonNull(tierPrice, "Tier price must not be null");
        if (tierPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tier price cannot be negative: " + tierPrice);
        }
        this.tierPrice = tierPrice.setScale(4, RoundingMode.HALF_UP);
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getPriceListCode() { return priceListCode; }
    public Integer getMinQuantity() { return minQuantity; }
    public BigDecimal getTierPrice() { return tierPrice; }
    public Instant getCreatedAt() { return createdAt; }
}
