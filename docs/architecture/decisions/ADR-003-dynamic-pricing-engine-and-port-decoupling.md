# ADR-003: Modular Monolith Port Decoupling and Dynamic Tiered Pricing Engine

- **Status:** Accepted
- **Date:** 2026-10-07
- **Context:** Catalog Subsystem, Inter-Module Isolation & B2B Commercial Pricing
- **Jira Reference:** `ERP-3`

---

## 1. Context & Problem Statement
When developing the `Product` catalog module, two critical architectural challenges were identified:

1. **Tight ORM Coupling vs. Dangling Reference Risk:**
   - Using traditional `@ManyToOne Organization organization` couples the persistence models of two distinct business modules, violating Modular Monolith bounded contexts and complicating future microservice extractions.
   - Conversely, merely storing a raw `Long organizationId` introduces the risk of orphaned products created against non-existent or inactive/suspended organizations.
2. **The Static Pricing Illusion:**
   - Storing a single static `basePrice` in the `Product` table fails enterprise B2B reality, where prices vary by customer classification (`RETAIL`, `WHOLESALE`, `VIP`), order volume thresholds, and contractual discount tiers.
3. **ArchUnit Rule Brittleness:**
   - Hardcoded class names in ArchUnit assertions created fragile checks prone to silent failures during refactoring.

---

## 2. Decision
1. **Loose ID Coupling with Validation Port (`OrganizationValidatorPort`):**
   - The `Product` entity stores only `organizationId: Long`.
   - Defined `OrganizationValidatorPort` interface in `com.erpium.erp.organization.port`.
   - `ProductService` injects the port and verifies that the target organization exists and is in `ACTIVE` status prior to saving any product.
2. **Dynamic Tiered Pricing Engine (`ProductPriceTier`):**
   - Retained `Product.basePrice` as the Catalog MSRP reference and fallback floor (scale = 4, precision = 19).
   - Created `ProductPriceTier` entity capturing `priceListCode`, `minQuantity`, and `tierPrice`.
   - Implemented `calculateEffectivePrice(productId, priceListCode, quantity)` in `ProductService`: dynamically resolves the highest qualifying volume tier, applies VAT tax rates, and rounds gross totals using `RoundingMode.HALF_UP` to scale 2.
3. **Generalized ArchUnit Inter-Module Isolation:**
   - Added automated rules forbidding `com.erpium.erp.product..` from depending on internal entities or repositories of `com.erpium.erp.organization`. Communication is strictly confined to public ports and DTOs.
   - Enforced unidirectional upstream dependencies (Organization must not depend on Product).

---

## 3. Consequences

### Positive:
- Clean Modular Monolith boundaries: zero ORM cross-module entity leaks.
- Zero dangling product creation: verified tenant validity at service boundaries.
- Flexible B2B tiered volume pricing supporting multiple commercial price lists.

### Negative / Trade-Offs:
- Requires synchronous in-process method invocation via Port (must evolve to domain event cache if split into distributed microservices; tracked as `DEBT-005`).
- Requires unique database constraint on `(product_id, price_list_code, min_quantity)` in Flyway to prevent overlapping pricing conflicts (tracked as `DEBT-001`).
