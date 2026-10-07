package com.erpium.erp.product;

/**
 * Enterprise lifecycle status for a Product.
 *
 * Lifecycle Transitions:
 * - DRAFT        -> ACTIVE (Product approved for sales and inventory operations)
 * - ACTIVE       -> DISCONTINUED (Product phase-out; cannot create new purchase or sales orders)
 * - DISCONTINUED -> Terminal state (Historical analytics and warranty tracking only)
 */
public enum ProductStatus {

    /**
     * Product is in preparation (catalog setup, pricing review). Not visible for sales.
     */
    DRAFT,

    /**
     * Active commercial product available for ordering, warehousing, and billing.
     */
    ACTIVE,

    /**
     * Discontinued product. Preserved for historical ledger and audit integrity.
     */
    DISCONTINUED
}
