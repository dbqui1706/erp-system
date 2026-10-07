package com.erpium.erp.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Entity representing an individual line item in a Sales Order.
 *
 * Architecture & Financial Invariant Compliance:
 * - INV-002: Line amount and tax amount remain unrounded (scale = 4) to eliminate
 *   line-level fractional rounding errors across multi-line aggregate orders.
 * - Modular Monolith: References product by productId (Long), avoiding direct cross-module JPA coupling.
 */
@Entity
@Table(name = "sales_order_lines")
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    /**
     * Unrounded internal line subtotal: unitPrice * quantity (scale = 4).
     */
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;

    /**
     * Unrounded internal line tax: lineAmount * taxRate (scale = 4).
     */
    @Column(name = "line_tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTaxAmount;

    protected SalesOrderLine() {
        // Required by JPA specification
    }

    public SalesOrderLine(SalesOrder order, Long productId, String sku, String productName,
                          Integer quantity, BigDecimal unitPrice, BigDecimal taxRate) {
        this.order = Objects.requireNonNull(order, "Order must not be null");
        this.productId = Objects.requireNonNull(productId, "Product ID must not be null");
        this.sku = Objects.requireNonNull(sku, "SKU must not be null");
        this.productName = Objects.requireNonNull(productName, "Product name must not be null");

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Line quantity must be at least 1 (got " + quantity + ")");
        }
        this.quantity = quantity;

        Objects.requireNonNull(unitPrice, "Unit price must not be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative: " + unitPrice);
        }
        this.unitPrice = unitPrice.setScale(4, RoundingMode.HALF_UP);

        Objects.requireNonNull(taxRate, "Tax rate must not be null");
        if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0.0 and 1.0 (got " + taxRate + ")");
        }
        this.taxRate = taxRate.setScale(4, RoundingMode.HALF_UP);

        // Compute unrounded line totals with full precision (scale = 4)
        this.lineAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).setScale(4, RoundingMode.HALF_UP);
        this.lineTaxAmount = this.lineAmount.multiply(this.taxRate).setScale(4, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public SalesOrder getOrder() { return order; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getLineAmount() { return lineAmount; }
    public BigDecimal getLineTaxAmount() { return lineTaxAmount; }
}
