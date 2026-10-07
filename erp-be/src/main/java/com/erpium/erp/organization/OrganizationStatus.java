package com.erpium.erp.organization;

/**
 * Enterprise lifecycle status for an Organization.
 * Replaces naive boolean flags with an explicit state machine:
 *
 * State Transitions:
 * - DRAFT      -> ACTIVE (Activation upon legal verification)
 * - ACTIVE     -> SUSPENDED (Temporary freeze due to policy/compliance/billing)
 * - ACTIVE     -> CLOSED (Permanent dissolution or decommission)
 * - SUSPENDED  -> ACTIVE (Reinstatement after resolution)
 * - SUSPENDED  -> CLOSED (Permanent closure while suspended)
 * - CLOSED     -> Terminal state (Strictly immutable; historical audit records only)
 */
public enum OrganizationStatus {

    /**
     * Newly registered organization undergoing configuration or approval.
     * Cannot participate in financial transactions yet.
     */
    DRAFT,

    /**
     * Fully operational organization authorized for all ERP transactions.
     */
    ACTIVE,

    /**
     * Temporarily frozen organization. Read-only access allowed for historical records;
     * new financial/inventory transactions are blocked.
     */
    SUSPENDED,

    /**
     * Permanently dissolved or archived organization. Terminal immutable state.
     */
    CLOSED
}
