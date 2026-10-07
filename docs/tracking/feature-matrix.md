# ERP Feature & Implementation Matrix

This matrix maps high-level business capabilities to codebase artifacts, test suites, and **Jira Epics / Stories**.

---

## Implemented Features (Release v2.3.0)

| Feature / Domain | Jira Issue | Core Entity | Primary API Endpoint | Test Verification | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Organization Tenant Management** | `ERP-101` | `Organization` | `GET /api/v1/organizations/{id}`<br>`POST /api/v1/organizations` | `OrganizationServiceTest` | **Done** |
| **Lifecycle State Machine** | `ERP-102` | `OrganizationStatus` | `POST /api/v1/organizations/{id}/activate`<br>`POST /api/v1/organizations/{id}/suspend`<br>`POST /api/v1/organizations/{id}/close` | `OrganizationServiceTest` | **Done** |
| **State Audit Trail** | `ERP-103` | `OrganizationStatusHistory` | `GET /api/v1/organizations/{id}/history` | `OrganizationServiceTest` | **Done** |
| **Product Catalog Management** | `ERP-201` | `Product` | `GET /api/v1/products/{id}`<br>`POST /api/v1/products` | `ProductServiceTest` | **Done** |
| **Catalog Pagination (Anti-OOM)** | `ERP-202` | `Product` | `GET /api/v1/products?page=0&size=20` | `ProductController` | **Done** |
| **Dynamic Tiered Price Engine** | `ERP-203` | `ProductPriceTier` | `POST /api/v1/products/{id}/price-tiers`<br>`GET /api/v1/products/{id}/effective-price` | `ProductServiceTest` | **Done** |
| **Inter-Module Port Decoupling** | `ERP-301` | `OrganizationValidatorPort` | Evaluated internally at Service boundary | `ProductServiceTest`<br>`ArchitectureTest` | **Done** |
| **Modular Monolith Guardrails** | `ERP-302` | N/A (ArchUnit) | Build verification `./gradlew test` | `ArchitectureTest` | **Done** |
| **Sales Order Subsystem** | `ERP-401` | `SalesOrder`, `SalesOrderLine` | `GET /api/v1/orders/{id}`<br>`POST /api/v1/orders` | `SalesOrderServiceTest` | **Done** |
| **Order-Level Rounding Engine** | `ERP-402` | `SalesOrder` | Evaluated internally on order calculation | `SalesOrderServiceTest` | **Done** |

---

## Upcoming Backlog (Recommended Jira Sprint 3)

| Feature / Domain | Proposed Jira Issue | Target Scope | Dependency |
| :--- | :--- | :--- | :--- |
| **Price Tier Unique Constraint** | `ERP-204` | Flyway Migration + Exception Handler mapping | `ERP-203` |
| **Multi-Tenancy Security Filter** | `ERP-501` | Spring Security JWT Tenant Context Resolver | `ERP-101` |
| **Inventory Reservation & Stock Lock** | `ERP-601` | Warehouse inventory allocation on order confirm | `ERP-401` |
