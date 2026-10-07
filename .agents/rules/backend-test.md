---
trigger: glob
description: "Testing standards: JUnit 5, Mockito, Testcontainers PostgreSQL, and test determinism."
globs: "erp-be/src/test/**"
---

# Backend Testing Guidelines

## 1. Test Architecture & Slicing
- **Unit Tests:** Focus on Service business logic using JUnit 5 + Mockito. Keep tests fast and isolated without loading the Spring application context.
- **Web Layer Tests:** Use `@WebMvcTest` for Controllers to verify request validation, security access rules, and RFC 7807 error status mappings.
- **Repository Tests:** Use `@DataJpaTest` with Testcontainers to verify custom queries and entity graph fetching.
- **Integration Tests:** Use `@SpringBootTest` sparingly for end-to-end multi-module workflows.

## 2. Testcontainers & Infrastructure Parity
- **Always use Testcontainers with PostgreSQL** for repository and integration testing.
- **Do NOT use H2 database**: H2 diverges from PostgreSQL regarding concurrency, locking, JSONB, and SQL dialect behavior.

## 3. Determinism & Test Quality
- Tests must be completely deterministic: no dependencies on local clock, system timezone, execution order, or shared mutable state. Inject a `Clock` bean whenever testing time-sensitive business logic.
- A bug fix must always include an automated regression test reproducing the issue before the fix.
- Test both happy paths and failure scenarios (validation errors, unauthorized access, concurrency conflicts).
- Run tests via `./erp-be/gradlew -p erp-be test`. Never fabricate test results.
