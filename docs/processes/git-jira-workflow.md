# Engineering Workflow: Git, Jira & Living Docs Integration

This document outlines the collaborative engineering workflow connecting **Jira tracking**, **Git version control**, and **Docs-as-Code**.

---

## 1. Git Commit Message Conventions

Every commit must reference an active Jira Issue Key in its subject line:

```text
[JIRA-KEY] <type>(<scope>): <concise description in English>

[Optional body explaining context and non-obvious rationale]
```

### Commit Types:
- `feat`: A new user-facing business feature (e.g. `[ERP-203] feat(product): implement dynamic volume tiered price engine`).
- `fix`: A bug fix or invariant correction (e.g. `[ERP-204] fix(pricing): add unique constraint on price tiers`).
- `refactor`: Code change that neither fixes a bug nor adds a feature.
- `docs`: Documentation updates, ADR additions, or dictionary expansions.
- `test`: Adding or refactoring unit, slice, or architecture test cases.

---

## 2. Branching Strategy
- `main`: Production-ready, stable codebase. Direct commits prohibited.
- `feat/<JIRA-KEY>-<short-description>`: Feature development branches (e.g. `feat/ERP-203-dynamic-pricing`).
- `fix/<JIRA-KEY>-<short-description>`: Bugfix branches.

---

## 3. Two-Factor Governance & Automated Enforcement (v2.6.0)

To resolve the vulnerability where local Git hooks can be bypassed or forgotten on fresh repository clones, the ERP platform implements a **Two-Factor Enforcement Architecture**:

### Factor 1: Client-Side Versioned Hooks & Zero-Friction Binding
- **Versioned Directory:** All hook scripts reside in the version-controlled `.githooks/` directory (`commit-msg`, `pre-push`).
- **Zero-Friction Binding via Gradle:** 
  The Gradle build script (`erp-be/build.gradle`) defines the task `installGitHooks`, wired directly to the `compileJava` lifecycle.
  ```bash
  # Simply compiling or testing automatically points Git to .githooks and grants execution permissions:
  ./erp-be/gradlew test
  # Or manually:
  ./erp-be/gradlew installGitHooks
  ```
- **Anti-Bypass `pre-push` Hook:** Even if a developer bypasses local validation using `git commit --no-verify`, the `.githooks/pre-push` hook analyzes all outgoing commit SHAs prior to transmission and aborts the push if any commit lacks a valid Jira prefix.

### Factor 2: Server-Side CI Hard Gate (GitHub Actions)
- **Workflow:** `.github/workflows/verify-jira-commits.yml` executes on every `pull_request` event (`opened`, `edited`, `synchronize`, `reopened`) and `push` to protected branches (`main`, `develop`).
- **Flexible Squash-Merge Policy (3-B):**
  1. **PR Title Mandatory:** Every PR Title must match `^\[ERP-[0-9]+\]:? .+$`.
  2. **Squash-and-Merge Friendly:** If the PR Title contains a valid Jira Issue Key, individual experimental/micro commits on the branch are permitted with a warning. When merged using GitHub's **Squash and Merge**, the squashed commit on `main` automatically inherits the valid PR Title, preserving a clean 100% audit trail.
  3. **Mainline Push Validation:** Any direct push or rebased merge onto `main` or `develop` must have 100% of commits adhering to `[ERP-XXX]`. Violations fail the build immediately.

---

## 4. Definition of Done (DoD) Checklist

A Jira task or Git Pull Request cannot be marked **Done** unless all of the following criteria are satisfied:
- [ ] **Compilation & Formatting:** Code compiles cleanly with zero warnings (`./gradlew build`).
- [ ] **Test Coverage:** All unit tests and ArchUnit rules pass (`./gradlew test`).
- [ ] **Git Hook Compliance:** Local hooks installed via `./gradlew installGitHooks` and passing; PR Title formatted with active `[ERP-XXX]`.
- [ ] **Living Documentation:** 
  - If a major architectural design decision was made -> Added an ADR under `docs/architecture/decisions/`.
  - If new business terms or formulas were introduced -> Updated `docs/domain/ubiquitous-language.md`.
  - If technical trade-offs or edge-cases were deferred -> Logged in `docs/tracking/technical-debt-ledger.md`.
- [ ] **English Integrity:** All identifiers, comments, commit messages, and documentation written in English.
- [ ] **Security:** No hardcoded secrets, test credentials, or raw unpaginated list endpoints.
