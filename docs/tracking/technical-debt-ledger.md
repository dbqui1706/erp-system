# Technical Debt & Architecture Backlog Ledger

This ledger systematically tracks architectural trade-offs, deferred optimizations, and critical edge cases identified during architectural critiques. Each item is pre-formatted for synchronization into **Jira**.

---

## Active Technical Debt Inventory

### `DEBT-001`: Unique Constraint on Product Price Tiers
- **Category:** Data Integrity / Concurrency
- **Severity:** High
- **Jira Type:** `Bug` / `Task`
- **Context:** `ProductPriceTier` currently lacks a database unique constraint on `(product_id, price_list_code, min_quantity)`.
- **Risk:** Concurrent or duplicate administration entries could create conflicting tiers with the same volume threshold but differing prices, causing non-deterministic price lookups.
- **Remediation Plan:** Add Flyway migration with `ALTER TABLE product_price_tiers ADD CONSTRAINT uq_product_tier UNIQUE (product_id, price_list_code, min_quantity);`.

---

### `DEBT-002`: Order-Level vs. Line-Level Rounding Engine
- **Category:** Financial Precision / Taxation
- **Status:** **RESOLVED (v2.5.0 - Jira ERP-402)**
- **Severity:** High
- **Jira Type:** `Story`
- **Resolution Summary:** Implemented in `SalesOrder.java` aggregate root. Line items (`SalesOrderLine`) maintain unrounded line amounts and tax amounts with high precision (`scale = 4`). Final tax and gross totals are rounded with `RoundingMode.HALF_UP` to scale 2 only once at the aggregate order checkout level, eliminating fractional rounding discrepancies.

---

### `DEBT-003`: PostgreSQL Table Partitioning for Audit Logs
- **Category:** Infrastructure / Scalability
- **Severity:** Medium
- **Jira Type:** `Tech Debt`
- **Context:** `organization_status_histories` is strictly append-only.
- **Risk:** High-velocity status tracking across tens of thousands of tenants will swell table size into gigabytes, degrading index rebuilds and general backup speeds.
- **Remediation Plan:** Implement PostgreSQL declarative range partitioning by `created_at` (monthly or yearly partitions) and configure cold storage archival.

---

### `DEBT-004`: Multi-Currency Conversion & Exchange Rate Engine
- **Category:** Domain Capability
- **Severity:** Medium
- **Jira Type:** `Story`
- **Context:** `ProductService.calculateEffectivePrice()` evaluates prices solely in the product's native currency.
- **Risk:** In multi-currency transactions, payments in EUR/VND against USD catalogs cannot calculate dynamic conversions.
- **Remediation Plan:** Create `ExchangeRate` subsystem with daily rate feeds and spread calculations.

---

### `DEBT-005`: Event-Driven Cache for Organization Validation Port
- **Category:** Distributed Architecture / Decoupling
- **Severity:** Low (Becomes High if split to Microservices)
- **Jira Type:** `Architecture Spike`
- **Context:** `ProductService` invokes `OrganizationValidatorPort` via synchronous in-memory call.
- **Risk:** If Organization is extracted into a remote service, synchronous network calls introduce cascading latency and availability coupling.
- **Remediation Plan:** Implement local read-model cache in Product module synchronized via asynchronous Domain Events (`OrganizationActivatedEvent`, `OrganizationClosedEvent`).

---

### `DEBT-006`: Client-Side Git Hook Bypass Vulnerability
- **Category:** CI/CD / Process Governance / Compliance
- **Status:** **RESOLVED (v2.6.0)**
- **Severity:** Medium
- **Jira Type:** `Story`
- **Resolution Summary:** Solved local hook non-versioning and `--no-verify` bypass vulnerability by deploying Two-Factor Enforcement:
  1. Client-Side: Versioned `.githooks/` directory (`commit-msg` and `pre-push`) automatically bound via `installGitHooks` Gradle lifecycle task on `./gradlew test`/`build`.
  2. Server-Side: GitHub Actions workflow (`.github/workflows/verify-jira-commits.yml`) acting as unbypassable CI gate validating PR Title and commit history according to Flexible Squash-Merge Policy 3-B.
