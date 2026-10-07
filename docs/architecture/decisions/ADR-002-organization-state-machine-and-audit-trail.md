# ADR-002: Enterprise Organization Lifecycle State Machine and Immutable Audit Trail

- **Status:** Accepted
- **Date:** 2026-10-06
- **Context:** Organization Foundation & Regulatory Compliance
- **Jira Reference:** `ERP-2`

---

## 1. Context & Problem Statement
The initial implementation of `Organization.java` used a naive `boolean active` flag for lifecycle management. In enterprise ERP systems, primitive boolean flags introduce severe operational risks:
1. **The Soft-Delete Trap:** Setting `active = false` without business context creates foreign key ambiguities. Unique constraints (such as organization `code`) fail when attempting to create a new organization with the same code as a deactivated record.
2. **Missing Business Semantics:** Enterprise entities transition through distinct phases (`DRAFT`, `ACTIVE`, `SUSPENDED`, `CLOSED`). A boolean cannot represent regulatory suspensions or permanent legal dissolutions.
3. **Absence of Auditability:** Changes in state lacked tracking regarding who authorized the transition, when it took place, and the underlying legal/business justification.
4. **Manual DTO Mapping Overhead:** Hand-written `.fromEntity()` mappers introduced boilerplate and risk of `LazyInitializationException` outside transactions.

---

## 2. Decision
1. **Adopt a 4-State Enterprise State Machine:**
   - Introduced `OrganizationStatus` enum: `DRAFT`, `ACTIVE`, `SUSPENDED`, `CLOSED`.
   - Encapsulated transition rules directly inside the `Organization` entity:
     * `activate()`: Permitted from `DRAFT` or `SUSPENDED`.
     * `suspend()`: Permitted only from `ACTIVE`.
     * `close()`: Permitted from `ACTIVE` or `SUSPENDED`.
     * `CLOSED` is permanently immutable: all mutations or reactivation attempts throw `IllegalStateException`.
2. **Immutable Audit Trail (`OrganizationStatusHistory`):**
   - Created an append-only audit entity recording `organizationId`, `fromStatus`, `toStatus`, `reason`, and `createdAt`.
   - Guaranteed that every state transition records an audit log within the same database transaction.
3. **Compile-Time DTO Automation with MapStruct:**
   - Integrated MapStruct 1.6.3 to generate zero-reflection mapping code compiling directly into immutable Java 21 `record` constructors (`OrganizationResponse`).

---

## 3. Consequences

### Positive:
- Guaranteed legal and financial state consistency across tenant lifecycles.
- Complete regulatory audit trail for accounting compliance and security investigations.
- High-performance, zero-overhead compile-time DTO transformations.

### Negative / Trade-Offs:
- State history table will grow indefinitely (append-only); requires future PostgreSQL table partitioning strategy (tracked in Technical Debt Ledger as `DEBT-003`).
