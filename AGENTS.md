# Repository Guidelines

## Project Structure & Module Map
- Maven aggregator `pom.xml` links modules: `core` (annotations, execution modes), `cluster` (cluster manager), `metadata-store` (metadata access), `messaging` (message bus), `source`/`handlers`/`pipeline`/`sink` (ingestion and processing abstractions), `workflow` (workflow DSL and engine), and `spring-boot-starter` (auto-config and registries). 
- Source lives in `*/src/main/java`; add tests under `*/src/test/java`. Keep module-specific assets alongside code to preserve isolation.
- Spring Boot 3.3 and Reactor 3.6 are managed from the root; modules consume internal artifacts via the parent POM.

## Build, Test, and Development Commands
- `mvn clean install` (root): build all modules, run unit tests.
- `mvn -pl <module> -am test`: build and test a specific module plus its dependencies, e.g., `mvn -pl workflow -am test`.
- `mvn -pl <module> -DskipTests package`: package a single module when you need speed.
- `mvn -pl spring-boot-starter spring-boot:repackage`: produce the starter jar with bundled dependencies for downstream apps.

## Coding Style & Naming Conventions
- Target Java 17; use 4-space indentation and standard Java brace placement. Package names stay lowercase (`com.ees.framework.<area>`); classes/interfaces use PascalCase; methods and fields use camelCase.
- Prefer constructor injection for Spring components; keep annotations from `core` (e.g., `@FxSource`, `@FxSink`, `@FxPipelineStep`) close to the classes they mark.
- Keep public APIs small and reactive-friendly (Flux/Mono) where applicable; avoid blocking in Reactor pipelines.

## Testing Guidelines
- Default to JUnit 5 (Spring Boot starter pulls it transitively); place tests under the module they cover with `*Test` suffix. For reactive flows, add Reactor Test (`StepVerifier`) to assert sequences deterministically.
- Use `mvn test` at the module or root level before sending PRs; add focused runs with `-Dtest=ClassNameTest` when iterating.
- Favor fast unit tests over heavy integration; if you must use Spring context, minimize configuration and reuse common test fixtures per module.

## Commit & Pull Request Guidelines
- Follow a Conventional Commits style (`feat:`, `fix:`, `chore:`) to keep history searchable; write imperative, present-tense subjects.
- PRs should describe scope, list touched modules, reference issues, and include before/after notes or logs (test output is enough; screenshots only if UI is involved).
- Keep changes small and modular; new features should ship with tests and minimal documentation updates in the affected module.

## Security & Configuration Tips
- Never commit secrets; externalize credentials via environment or application properties in downstream apps.
- Ensure new components respect module boundaries (e.g., keep Spring Boot wiring inside `spring-boot-starter`; avoid cross-module shortcuts that bypass public APIs).
