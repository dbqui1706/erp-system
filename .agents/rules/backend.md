---
trigger: glob
description: "Core architectural boundaries, ERP invariants, and non-negotiables for Java 21 Spring Boot backend."
globs: "erp-be/**"
---

# Backend Core Principles (ERP System)

> Applies to all files under `erp-be/`. Cross-cutting standards (API contracts, RFC 7807 error format, DoD) are governed by `common.md`. Specialized rules are in `backend-jpa.md`, `backend-api.md`, and `backend-test.md`.

## 1. Architectural Boundaries & Layering
- Strict layer flow: `Controller -> Service -> Repository`. Controllers never invoke Repositories directly.
- Modular monolith by domain first: Modules communicate strictly via public Service interfaces; never import another module's internal Entities or Repositories directly.
- Strict DTO decoupling: Never expose JPA entities outside the Service boundary. Use Java `record` for Request/Response DTOs.
- Use constructor injection only. Field injection with `@Autowired` is prohibited.

## 2. ERP Domain Invariants & Data Integrity
- **Financial & Ledger Immutability:** General Ledger entries and financial audit lines are append-only. Never update or delete posted transactions; corrections must be posted as offsetting adjustment entries.
- **Inventory & Balance Concurrency:** Critical state mutations (stock deduction, balance updates) must use explicit concurrency controls (optimistic locking `@Version` or pessimistic locking) to prevent race conditions.
- **Monetary Precision:** Always use `BigDecimal` with explicit scale and rounding mode (e.g. `RoundingMode.HALF_UP`). Never use `double` or `float` for money or quantities.
- **Multi-Tenancy & Authorization:** Always enforce organization / tenant ownership checks at the Service boundary to prevent Insecure Direct Object References (IDOR). A valid user role alone is never sufficient.

## 3. Transactions & External I/O
- Place `@Transactional` exclusively on Service methods.
- Mark read queries with `@Transactional(readOnly = true)`.
- **Never execute external I/O** (HTTP client calls, messaging brokers, emails, heavy file storage) inside an active database transaction.

## 4. Verification
- Verify all backend changes by running `./erp-be/gradlew -p erp-be test`. Never declare tasks complete without running tests.
