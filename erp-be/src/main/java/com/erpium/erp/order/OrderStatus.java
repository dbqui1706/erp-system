package com.erpium.erp.order;

/**
 * Enterprise lifecycle status for a Sales Order.
 *
 * State Machine Transitions:
 * - DRAFT      -> CONFIRMED (Order review passed; inventory allocation initiated)
 * - DRAFT      -> CANCELLED (Customer aborted or expired draft)
 * - CONFIRMED  -> FULFILLED (Goods delivered / service rendered; ready for billing)
 * - CONFIRMED  -> CANCELLED (Order voided prior to fulfillment)
 * - FULFILLED  -> Terminal state (Strictly immutable; posted financial audit trail)
 * - CANCELLED  -> Terminal state (Strictly immutable)
 */
public enum OrderStatus {

    /**
     * Initial editable draft order. Line items can be added, modified, or removed.
     */
    DRAFT,

    /**
     * Legally binding confirmed commercial order. Lines are locked; awaiting fulfillment.
     */
    CONFIRMED,

    /**
     * Completed commercial order. Goods delivered. Ready for General Ledger invoice posting.
     */
    FULFILLED,

    /**
     * Cancelled order. Terminal audit state.
     */
    CANCELLED
}
