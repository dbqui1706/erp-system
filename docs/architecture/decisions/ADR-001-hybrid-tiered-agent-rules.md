# ADR-001: Hybrid Tiered Architecture for AI Agent Rule Governance

- **Status:** Accepted
- **Date:** 2026-10-06
- **Context:** Project Bootstrap & AI Context Optimization
- **Jira Reference:** `ERP-1`

---

## 1. Context & Problem Statement
During initial development, the backend rule set in `.agents/rules/backend.md` was a monolithic document spanning 117 lines and over 50 discrete micro-rules. Whenever an AI coding agent performed any task under `erp-be/**`, this entire file was injected into the prompt context.

This caused severe operational issues:
1. **Cognitive Load & Attention Decay (Lost-in-the-Middle):** LLMs suffered from instruction degradation when long conversation transcripts, git diffs, and compiler logs diluted static prompts.
2. **Blurring of Boundaries:** The prompt contained rules that compilers and linters could easily enforce deterministically (e.g., banning field `@Autowired`, enforcing package layering, forbidding `System.out.println`).
3. **Token Inefficiency:** Over 2,500 tokens were consumed on every tool step regardless of whether the agent touched a Controller, a Test, or a YAML configuration file.

---

## 2. Decision
We adopted a **Hybrid 2-Tier Modular Rules Architecture**:

1. **Tier 1 - Core Golden Rules (`backend.md`):**
   - Condensed to ~37 lines (< 1,000 tokens).
   - Injected globally for `erp-be/**`.
   - Focuses strictly on high-level architectural boundaries (Controller -> Service -> Repository), multi-tenancy IDOR checks, transaction boundaries, and immutable domain invariants.
2. **Tier 2 - Specialized Context-Triggered Rules:**
   - `backend-jpa.md`: Triggered only for `erp-be/**/{entity,repository,domain}/**`. Governs `FetchType.LAZY`, N+1 prevention, `@Version`, and Flyway migrations.
   - `backend-api.md`: Triggered only for `erp-be/**/{controller,dto}/**`. Governs thin controllers, Java `record` DTOs, Bean validation, and pagination caps.
   - `backend-test.md`: Triggered only for `erp-be/src/test/**`. Governs Testcontainers PostgreSQL, time determinism, and test slicing.
3. **Single Source of Truth (`common.md`):**
   - Defines cross-cutting standards between Frontend and Backend: `/api/v1` routes, RFC 7807 `ProblemDetail`, camelCase JSON serialization, and Definition of Done.
4. **Tooling Offloading (ArchUnit):**
   - Syntax and package import rules were offloaded from prompts to automated compile-time assertions via `ArchUnit` (e.g. `no_field_injection`, `controllers_must_not_depend_on_repositories`).

---

## 3. Consequences

### Positive:
- Reduced token consumption by ~70% per turn.
- Improved AI instruction adherence to > 95% on core rules.
- Deterministic architectural verification via `./gradlew test`.

### Negative / Trade-Offs:
- Requires consistent package naming conventions to match glob patterns.
- Rule changes require maintenance across multiple files (managed via `RULES_CHANGELOG.md`).
