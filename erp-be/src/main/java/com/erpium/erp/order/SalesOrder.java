package com.erpium.erp.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root representing an enterprise Sales Order.
 *
 * Architecture & Financial Invariant Compliance:
 * - INV-002 & DEBT-002 Resolution: Implements Order-Level Rounding Engine. Sums unrounded line totals
 *   and rounds tax/gross totals ONLY ONCE at the aggregate invoice level with RoundingMode.HALF_UP.
 * - Modular Monolith: References Organization via organizationId (Long), validated via Port.
 * - Concurrency: Protected with optimistic locking via @Version.
 */
@Entity
@Table(name = "sales_orders")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.DRAFT;

    /**
     * Unrounded internal sum of all lines before tax (scale = 4).
     */
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    /**
     * Unrounded internal total VAT/tax (scale = 4).
     */
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    /**
     * Final commercial invoice total rounded with HALF_UP to scale 2.
     * Complies with financial legal requirements (Order-Level Rounding).
     */
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SalesOrderLine> lines = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SalesOrder() {
        // Required by JPA specification
    }

    public SalesOrder(Long organizationId, String orderNumber, String customerName, String currency) {
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID must not be null");
        this.orderNumber = Objects.requireNonNull(orderNumber, "Order number must not be null").trim().toUpperCase();
        this.customerName = Objects.requireNonNull(customerName, "Customer name must not be null").trim();
        this.currency = Objects.requireNonNull(currency, "Currency must not be null").trim().toUpperCase();
        this.status = OrderStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // LINE MANAGEMENT & ORDER-LEVEL ROUNDING ENGINE (DEBT-002 Resolution)
    // =========================================================================

    /**
     * Adds an order line and recalculates order financial totals using Order-Level Rounding.
     */
    public void addLine(Long productId, String sku, String productName, Integer quantity,
                        BigDecimal unitPrice, BigDecimal taxRate) {
        if (this.status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Lines can only be added to DRAFT orders. Current status: " + this.status);
        }

        SalesOrderLine line = new SalesOrderLine(this, productId, sku, productName, quantity, unitPrice, taxRate);
        this.lines.add(line);
        recalculateTotals();
    }

    /**
     * Order-Level Rounding Algorithm (INV-002):
     * 1. Sum unrounded line amounts (scale = 4).
     * 2. Sum unrounded line tax amounts (scale = 4).
     * 3. Apply commercial RoundingMode.HALF_UP (scale = 2) ONLY ONCE to the final gross total.
     */
    private void recalculateTotals() {
        BigDecimal sumSubtotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal sumTax = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        for (SalesOrderLine line : this.lines) {
            sumSubtotal = sumSubtotal.add(line.getLineAmount());
            sumTax = sumTax.add(line.getLineTaxAmount());
        }

        this.subtotalAmount = sumSubtotal;
        this.taxAmount = sumTax;
        this.totalAmount = sumSubtotal.add(sumTax).setScale(2, RoundingMode.HALF_UP);
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // STATE MACHINE TRANSITIONS
    // =========================================================================

    public void confirm() {
        if (this.status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT orders can be confirmed. Current status: " + this.status);
        }
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an order with zero lines.");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void fulfill() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED orders can be fulfilled. Current status: " + this.status);
        }
        this.status = OrderStatus.FULFILLED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == OrderStatus.FULFILLED) {
            throw new IllegalStateException("Cannot cancel an order that has already been FULFILLED.");
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already CANCELLED.");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // ACCESSORS
    // =========================================================================

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<SalesOrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
