# AI Rule Governance & Evolution Framework

This document tracks all version changes, design rationales, and governance policies for AI coding rules in `.agents/rules/`.

---

## 1. Governance Principles: Rule Hygiene Framework

To prevent rule degradation, prompt bloat, and conflicting instructions over time, every modification to `.agents/rules/` must satisfy the **Rule Hygiene Checklist**:

### The "3 NOs" (Nguyên tắc 3 KHÔNG):
1. **DO NOT put automated linting/compiler checks into prompts:**
   - If an automated tool (ArchUnit, Checkstyle, Spotless, Flyway, Compiler) can enforce a rule deterministically, implement it in code/CI. Never waste LLM attention tokens on syntax/import bans.
2. **DO NOT exceed the 50-line cognitive threshold in Core Rules:**
   - `backend.md` (Tier 1) must remain under ~50 lines (< 1,000 tokens). If new instructions are needed, evaluate whether they belong to a domain module or a specialized triggered file in Tier 2.
3. **DO NOT contradict `common.md`:**
   - `common.md` is the universal single-source-of-truth. No language-specific rule file may redefine error schemas (RFC 7807), casing conventions, or API URL structures.

### Pre-Update Verification Checklist:
- [ ] Is this rule non-negotiable across the entire project or domain?
- [ ] Can this rule be verified deterministically via an automated test or linter instead?
- [ ] Does the glob pattern trigger only for the relevant file types/paths?
- [ ] Are all identifiers, comments, and instructions written in English?
- [ ] Has the change been logged in the Changelog below with version and rationale?

---

## 2. Version Changelog

### [v2.6.0] - 2026-10-07 (Two-Factor Jira Git Enforcement & Automated Hook Provisioning)
- **Eliminated Client-Side Hook Limitation (Critique #3 & DEBT-006 Resolved):** Solved non-versioning and `--no-verify` bypass vulnerabilities by adopting a Two-Factor Governance model.
- **Client-Side Automation (`.githooks/`):** Version-controlled `commit-msg` and `pre-push` hooks. Created Gradle lifecycle task `installGitHooks` attached to `compileJava`, automatically setting `git config core.hooksPath .githooks` and granting execute permissions without manual developer intervention.
- **Server-Side CI Hard Gate (`verify-jira-commits.yml`):** Implemented GitHub Actions workflow validating Pull Request titles and commit history. Implemented Flexible Squash-Merge Policy (3-B) enforcing PR Title compliance while warning on non-conforming intermediate branch commits.
- **Living Documentation & Process Hardening:** Updated `docs/processes/git-jira-workflow.md` with Two-Factor Governance architecture and updated `docs/tracking/technical-debt-ledger.md` (DEBT-006).

### [v2.5.0] - 2026-10-07 (Sales Order Subsystem & Automated Git Hook)
- **Sales Order Aggregate & State Machine (ERP-401):** Implemented `SalesOrder` subsystem (`DRAFT`, `CONFIRMED`, `FULFILLED`, `CANCELLED`) with inter-module port validation.
- **Order-Level Rounding Engine (DEBT-002 Resolved):** Eliminated fractional line rounding drift by maintaining high-precision scale = 4 line amounts, applying `HALF_UP` scale = 2 rounding once at order checkout.
- **Git Commit Enforcement:** Implemented `scripts/hooks/commit-msg` requiring active Jira Issue Key regex `^[ERP-XXX]` on all commits.
- **ArchUnit Encapsulation:** Added rule preventing `order` module from accessing internal entities/repositories of `organization` or `product`.

### [v2.4.0] - 2026-10-07 (Holistic Docs-as-Code & Jira Integration Framework)
- **Living Documentation Architecture:** Established `docs/` knowledge base spanning Architecture (ADR-001, ADR-002, ADR-003), Domain (Ubiquitous Language, Business Invariants), Tracking (Feature Matrix, Tech Debt Ledger), and Processes.
- **Jira Integration Protocol:** Formalized commit conventions (`[ERP-XXX] <type>: <description>`) and aligned all technical debt items with Jira ticket categories.
- **Definition of Done Hardening:** Embedded mandatory Living Docs synchronization in `common.md` ensuring architecture decisions and domain rules remain persistent across AI sessions.

### [v2.3.0] - 2026-10-07 (Dynamic Pricing Engine, Audit Trail & Inter-Module Port)
- **Eliminated Static Pricing Trap:** Implemented `ProductPriceTier` and Dynamic Pricing Engine (`calculateEffectivePrice`) supporting volume tiered thresholds and customer price lists, with fallback to Catalog MSRP.
- **Inter-Module Decoupling Port:** Established `OrganizationValidatorPort` contract in `com.erpium.erp.organization.port`, eliminating orphaned product creation by enforcing active tenant validation prior to persistence.
- **State Machine Audit Trail:** Implemented immutable `OrganizationStatusHistory` entity and repository to record audit trails (who, what, when, why) on every lifecycle transition.
- **Generalized Architecture Guardrails:** Extended `ArchitectureTest.java` with pattern-based inter-module isolation rules prohibiting direct access to raw internal entities or repositories between modules.

### [v2.2.0] - 2026-10-07 (Product Catalog & Inter-Module Boundary Enforcement)
- **Modular Monolith Decoupling:** Implemented `Product` module with loose ID coupling (`organizationId: Long`), strictly eliminating ORM entity references across module boundaries.
- **ArchUnit Inter-Module Guardrail:** Added `product_module_must_not_access_organization_entity` rule in `ArchitectureTest.java` preventing `product` package from directly importing `Organization` JPA entity.
- **High-Precision Financial Pricing:** Implemented 4-scale `BigDecimal` internal base price and tax rate, with dynamic `HALF_UP` gross price calculation (scale = 2) mapped via MapStruct expressions.

### [v2.1.0] - 2026-10-06 (State Machine & MapStruct Integration)
- **Lifecycle Modernization:** Replaced primitive `boolean active` with `OrganizationStatus` (4-state enterprise state machine: `DRAFT`, `ACTIVE`, `SUSPENDED`, `CLOSED`) with immutable terminal states.
- **DTO Automation:** Integrated MapStruct 1.6.3 for compile-time generation of DTO transformations for Java 21 records, eliminating manual mapper boilerplate.
- **Modular Monolith Slice Isolation:** Added ArchUnit `slices().matching("com.erpium.erp.(*)..").should().beFreeOfCycles()` to prevent circular domain package entanglements.

### [v2.0.0] - 2026-10-06 (Hybrid Tiered Architecture)
- **Architectural Shift:** Migrated from monolithic `backend.md` (117 lines, ~2.5k tokens) to a 2-Tier Modular Structure.
- **Created `common.md`:** Universal standards for API design (`/api/v1`), RFC 7807 `ProblemDetail`, camelCase JSON serialization, pagination envelopes, and Definition of Done.
- **Refactored `backend.md` (Tier 1):** Condensed to ~37 lines focusing strictly on architectural boundaries, ERP invariants (ledger immutability, inventory concurrency, `BigDecimal` precision, multi-tenant isolation), and test verification.
- **Created Tier 2 Specialized Rules:**
  - `backend-jpa.md`: Explicit `FetchType.LAZY`, N+1 prevention, `@Version` optimistic locking, Flyway migrations.
  - `backend-api.md`: Thin controllers, Java `record` DTOs, Bean validation, RFC 7807 error translation.
  - `backend-test.md`: Testcontainers PostgreSQL enforcement, time determinism (`Clock`), test slicing.
- **Tooling Offloading:** Delegated `@Autowired` prohibition, controller-to-repository access bans, and layer hierarchy to ArchUnit.

### [v1.0.0] - 2026-10-05 (Initial Baseline)
- Initial single-file guidelines in `backend.md` (117 lines), `database.md`, and `frontend.md`.
