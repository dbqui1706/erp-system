# General instructions for AI coding agents

Applies to the entire project. Before making changes, inspect the relevant code and configuration and follow existing patterns.

## Workflow
- Make only the changes needed for the request; do not modify or reformat unrelated areas.
- Reuse existing structures, libraries, and helpers. Do not add dependencies or technologies unless they are necessary and requested.
- If a request is ambiguous and the choice could affect the API, data, or user behavior, ask before deciding.
- Do not claim tests passed unless you actually ran them.
- All documentation, code comments, commit messages, and source code identifiers must be written in English.
- Modular rules for backend, database, and frontend are located in `.agents/rules/`.


## Java and backend
- The backend is in `erp-be/` and uses Java 21, Spring Boot, and Gradle. Follow the existing package structure and coding style.
- Preserve the current API unless the request explicitly requires a change. Update relevant tests when behavior changes.
- Validate input at the appropriate boundaries; do not swallow errors or return misleading success responses.
- When changing configuration or database handling, check the impact on PostgreSQL and running the application with Docker Compose.

## Configuration and sensitive data
- Never put credentials, tokens, or personal data in source code, logs, tests, or documentation.
- Use environment variables for environment-specific configuration. Do not copy secrets from `.env` into `.env.example`; use safe placeholders in the example file when needed.
- Do not perform destructive operations or risk database data loss unless explicitly requested.

## Testing and verification
- For backend changes, run tests from the project root with `./erp-be/gradlew -p erp-be test` when the environment allows.
- Prefer tests directly related to the change. If tests cannot be run, state the command and why.
- Review the final diff to ensure it contains no changes outside the scope of the request.
