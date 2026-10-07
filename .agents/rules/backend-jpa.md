---
trigger: glob
description: "Persistence standards, JPA/Hibernate mapping, N+1 query prevention, and Flyway migration rules."
globs: "erp-be/**/{entity,repository,domain}/**"
---

# JPA & Database Persistence Guidelines

## 1. Entity Mapping Standards
- Primary keys: `Long` (sequence) or `UUID`.
- Timestamps: `Instant` or `OffsetDateTime` (UTC).
- Currency & quantities: `BigDecimal` with explicit `@Column(precision = ..., scale = ...)`. Never use `double` or `float`.
- Associations: Explicitly set `FetchType.LAZY` on all relationships (`@ManyToOne` and `@OneToOne` default to `EAGER`, always override).
- No Lombok `@Data` on JPA entities. Implement `equals`/`hashCode` based on business keys or ID, and keep `toString` free of lazy associations.
- Entities must have a `protected` no-args constructor and minimal public setters; model domain state mutations through descriptive methods.
- Add `@Version` for optimistic concurrency control on entities prone to concurrent updates (inventory, order status, account balances).

## 2. Query Performance & N+1 Prevention
- Lazy loading alone does not resolve N+1 queries. Proactively use `@EntityGraph`, `JOIN FETCH`, or batch fetching (`@BatchSize`).
- Return projections or DTOs directly for read-heavy or reporting queries instead of loading full managed entities.
- Avoid native SQL queries unless required for database-specific optimization; when used, always parameterize inputs. String concatenation in queries is strictly prohibited.

## 3. Schema Migrations (Flyway)
- All schema changes must be versioned migrations in `src/main/resources/db/migration/`.
- Never modify or delete an already applied migration script.
- Set `spring.jpa.hibernate.ddl-auto` to `validate` (or `none`). Never use `update` or `create-drop` outside ephemeral tests.
- Declare foreign keys, unique constraints, and indexes in Flyway migrations, and mirror them in entity annotations where appropriate.
