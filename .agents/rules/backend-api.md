---
trigger: glob
description: "Web layer standards: Controller design, DTO mapping, input validation, and API contracts."
globs: "erp-be/**/{controller,dto}/**"
---

# API & Web Layer Guidelines

## 1. Controller Design
- Controllers are thin HTTP adapters: accept requests, trigger Bean Validation, invoke Service methods, and return DTOs with appropriate HTTP status codes.
- Zero business logic and zero direct database queries inside Controllers.
- All collection/search endpoints must accept `Pageable` and enforce a server-side maximum page size (e.g. max 100 items). Return the standardized page envelope defined in `common.md`.

## 2. Request & Response DTOs
- Prefer Java `record` for all DTOs.
- Never accept or return JPA entities in Controller signatures.
- Perform Entity <-> DTO conversions at the Service boundary or via MapStruct.
- Only allow filtering and sorting on explicit whitelisted fields to prevent query injection and unexpected resource exhaustion.

## 3. Validation & Exception Mapping
- Validate all incoming request bodies with `jakarta.validation` annotations (`@NotNull`, `@NotBlank`, `@Size`, `@DecimalMin`, etc.) combined with `@Valid`.
- Centralize exception translation using `@RestControllerAdvice`.
- Map domain exceptions to standard RFC 7807 `ProblemDetail` structures (see `common.md`).
- Never return HTTP 200 with error payloads or swallow validation errors.
