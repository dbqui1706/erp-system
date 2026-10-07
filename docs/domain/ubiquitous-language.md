# Ubiquitous Language (ERP Domain Glossary)

This glossary defines standard domain terms used across documentation, user stories, code identifiers, and database schemas. Every engineer and AI agent must adhere strictly to these definitions.

---

## 1. Multi-Tenancy & Organizational Hierarchy

| Domain Term | Code Identifier | Definition |
| :--- | :--- | :--- |
| **Organization / Tenant** | `Organization` | The top-level legal entity or business unit operating in the ERP. All operational records (products, orders, ledger lines) belong to an Organization. |
| **Organization Status** | `OrganizationStatus` | Finite lifecycle states of a tenant: `DRAFT` (registration), `ACTIVE` (operational), `SUSPENDED` (temporary freeze), `CLOSED` (permanent legal dissolution). |
| **Tenant Isolation (Anti-IDOR)** | N/A | The architectural guarantee that users and API tokens can only read or mutate records belonging to their authorized Organization ID. |

---

## 2. Product Catalog & Commercial Pricing

| Domain Term | Code Identifier | Definition |
| :--- | :--- | :--- |
| **SKU** | `sku` | Stock Keeping Unit. Unique alphanumeric commercial identifier for an inventory or catalog product across the entire system. |
| **Catalog Base Price (MSRP)** | `basePrice` | Manufacturer's Suggested Retail Price. The default standalone unit price stored with 4 decimal places (`scale = 4`) used when no specific commercial contract or volume tier applies. |
| **Price List Code** | `priceListCode` | Commercial segmentation category for pricing (e.g. `RETAIL`, `WHOLESALE`, `VIP_PARTNER`, `EMPLOYEE`). |
| **Price Tier** | `ProductPriceTier` | Volume-based threshold price entry defining minimum quantity (`minQuantity`) required to unlock a discounted `tierPrice`. |
| **Dynamic Price Engine** | `calculateEffectivePrice` | Domain logic that evaluates order quantity and price list code to select the optimal unit price and compute gross VAT totals. |
| **Gross Price** | `grossPrice` | Total price after applying tax (`basePrice * (1 + taxRate)`), rounded to 2 decimal places using `RoundingMode.HALF_UP`. |

---

## 3. Financial & Accounting Concepts

| Domain Term | Context | Definition |
| :--- | :--- | :--- |
| **Ledger Immutability** | General Ledger | Accounting rule mandating that posted financial lines cannot be updated or deleted (`UPDATE`/`DELETE` prohibited). Reversals must be executed via balanced offset adjusting entries. |
| **Line-Level Rounding** | Invoicing / Pricing | Rounding gross values on each individual line item independently (`scale = 2`). |
| **Order-Level Rounding** | Invoicing / Taxation | Summing unrounded line totals with full precision (`scale = 4`) and applying tax/rounding only once at invoice checkout. Essential to eliminate fractional currency discrepancy. |
| **Terminal State** | State Machines | A status from which no forward or backward transitions are permitted (e.g. `CLOSED` for Organization, `DISCONTINUED` for Product). |
