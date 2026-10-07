---
trigger: model_decision
description: "Universal standards across frontend and backend: API contracts, RFC 7807 error handling, JSON conventions, and Definition of Done."
---

# Common Project Guidelines

> Universal single-source-of-truth across `erp-be/` and `erp-fe/`. If a specific rule contradicts `common.md`, `common.md` wins.

## 1. API Contract & URL Conventions
- REST endpoints start with `/api/v1/`.
- Resource names are plural nouns in hyphen-case (e.g. `/api/v1/purchase-orders`, `/api/v1/inventory-items`).
- Standard HTTP methods:
  - `GET`: Idempotent read. Return `200 OK`.
  - `POST`: Create resource. Return `201 Created` with `Location` header or created resource DTO.
  - `PUT`: Full update / replace. Return `200 OK`.
  - `PATCH`: Partial update. Return `200 OK`.
  - `DELETE`: Remove resource. Return `204 No Content` or `200 OK` (for soft delete status).
- Never return `200 OK` for an operation that failed or encountered business rule violations.

## 2. Error Format (RFC 7807 Problem Details)
- All 4xx and 5xx responses must adhere to RFC 7807 `application/problem+json` format:
  ```json
  {
    "type": "https://api.erp.local/errors/ORDER_ALREADY_CLOSED",
    "title": "Conflict",
    "status": 409,
    "detail": "Purchase order PO-2026-001 is already finalized and cannot be modified.",
    "instance": "/api/v1/purchase-orders/102",
    "code": "ERR_PURCHASE_ORDER_CLOSED",
    "timestamp": "2026-10-06T05:15:30Z",
    "traceId": "c8a4f91e32d1",
    "errors": [
      {
        "field": "status",
        "message": "Status transition from CLOSED to DRAFT is invalid"
      }
    ]
  }
  ```
- Error codes (`code`) must be stable, uppercase snake_case strings for frontend localization and automated handling.
- Never leak database stack traces, SQL syntax errors, or internal class names in client-facing error responses.

## 3. Data Representation & Serialization
- JSON keys: strictly `camelCase`.
- Timestamps: ISO-8601 in UTC (`YYYY-MM-DDTHH:mm:ssZ`).
- Monetary and high-precision amounts: serialize as numeric strings or exact decimals; never float approximations.
- Collections: always return paginated envelopes for list endpoints:
  ```json
  {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "isLast": true
  }
  ```

## 4. Definition of Done (DoD)
- Code compiles cleanly with zero warnings/linter errors (`./gradlew build`).
- Tests covering happy paths and critical edge cases pass locally (`./gradlew test`).
- Schema migrations (if any) are committed, tested, and backward-compatible.
- All code identifiers, comments, commit messages, and documentation are written in English.
- Commit messages follow the Jira integration format: `[ERP-XXX] <type>: <description>`.
- **Living Documentation Sync:** Any major architectural decision must add an ADR under `docs/architecture/decisions/`, new domain terms must update `docs/domain/ubiquitous-language.md`, and deferred trade-offs must be logged in `docs/tracking/technical-debt-ledger.md`.
- No hardcoded credentials, test tokens, or environment-specific hosts committed to the repository.
