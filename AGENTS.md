# Repository Guidelines

## Project Structure & Module Organization
- Maven aggregator at root (`pom.xml`) with modules: `core`, `cluster`, `metadata-store`, `messaging`, `source`, `handlers`, `pipeline`, `sink`, `workflow`, `spring-boot-starter`, `application`.
- Source code lives under `*/src/main/java`; tests under `*/src/test/java`. Keep module-specific assets alongside their code to maintain isolation.
- `spring-boot-starter` provides auto-config and registries; `application` is the sample app (UI stub under `application/src/main/resources/static`).

## Build, Test, and Development Commands
- `mvn clean install` — builds all modules and runs unit tests.
- `mvn -pl <module> -am test` — build + test a specific module and its deps (e.g., `mvn -pl workflow -am test`).
- `mvn -pl <module> -DskipTests package` — fast package for a single module.
- `mvn -pl spring-boot-starter spring-boot:repackage` — produce the starter jar with dependencies bundled.

## Coding Style & Naming Conventions
- Target Java 21; use 4-space indentation and standard Java brace placement.
- Packages lowercase (`com.ees.framework.<area>`); classes/interfaces PascalCase; methods/fields camelCase.
- Prefer constructor injection for Spring components; keep core annotations (`@FxSource`, `@FxSink`, `@FxPipelineStep`, etc.) close to the classes they mark.
- Favor reactive patterns (Reactor `Flux`/`Mono`); avoid blocking inside reactive flows.

## Testing Guidelines
- Use JUnit 5 (pulled via Spring Boot). For reactive sequences, use Reactor Test (`StepVerifier`) for deterministic assertions.
- Name tests with `*Test` and place them in the corresponding module’s `src/test/java`.
- Common commands: `mvn test` (root or module), or `mvn -Dtest=ClassNameTest test` for focused runs.
- Prefer fast unit tests; if a Spring context is required, minimize configuration and reuse fixtures.

## Commit & Pull Request Guidelines
- Follow Conventional Commits (`feat:`, `fix:`, `chore:`, etc.) in imperative, present tense.
- PRs should describe scope, list touched modules, reference issues, and note before/after behavior (logs are fine; screenshots only if UI changes).
- Keep changes small and modular; accompany new features with tests and minimal docs updates in affected modules.

## Security & Configuration Tips
- Do not commit secrets; externalize credentials via environment/application properties consumed by downstream apps (`spring.ai.*`, cluster configs, etc.).
- Respect module boundaries: keep Spring Boot wiring inside `spring-boot-starter`; avoid cross-module shortcuts bypassing public APIs.
- Add tool/command safeguards (whitelists, rate limits, audit logs) when wiring MCP/AI integrations.***
