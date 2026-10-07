# Business Invariants Matrix (Non-Negotiable ERP Rules)

This document catalogs non-negotiable economic, regulatory, and architectural invariants that must NEVER be violated by any feature implementation, database migration, or bug fix.

---

## 1. Core Invariants Matrix

| Invariant ID | Rule Title | Architectural Expression | Enforcement Mechanism |
| :--- | :--- | :--- | :--- |
| **INV-001** | **Financial & Ledger Immutability** | Posted accounting entries and state transition histories are append-only. Zero `UPDATE` or `DELETE` allowed. Corrections must be balancing offset lines. | Database permissions, JPA entities without setters for audited fields. |
| **INV-002** | **Monetary Calculation Precision** | All monetary amounts and tax rates must use `BigDecimal` with explicit scale. Prohibit `float` and `double`. High-precision storage (`scale = 4`); commercial display (`scale = 2, HALF_UP`). | Compiler assertions, Flyway `NUMERIC(19,4)` column definitions, unit tests. |
| **INV-003** | **Zero Dangling Multi-Tenant Records** | No operational entity (Product, Order, Inventory) can be persisted against an Organization ID that is non-existent, `SUSPENDED`, or `CLOSED`. | `OrganizationValidatorPort` evaluated at service boundary prior to persistence. |
| **INV-004** | **Optimistic Concurrency on Shared State** | Any entity subject to concurrent modifications (Inventory quantities, Organization details, Product catalog) must maintain a `@Version` column to prevent Lost Updates. | JPA `@Version private Long version;`, ArchUnit inspection. |
| **INV-005** | **Terminal State Inviolability** | Once an entity reaches a terminal state (`OrganizationStatus.CLOSED`, `ProductStatus.DISCONTINUED`), all mutation or reactivation operations must be rejected. | Encapsulated domain methods throwing `IllegalStateException`. |

---

## 2. Invariant Verification Checklist for Pull Requests
Before merging any PR or approving an AI-generated code block:
- [ ] Does any new entity handle monetary amounts? -> Verified `BigDecimal` with `RoundingMode.HALF_UP`?
- [ ] Does the entity belong to an Organization? -> Verified validation via `OrganizationValidatorPort`?
- [ ] Can the entity be updated concurrently? -> Verified `@Version` column exists?
- [ ] Are state transitions protected by domain invariants rather than public setters?
