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
- Migration in progress: move to Spring MVC + virtual threads; new APIs should be blocking-first. Reactive types are allowed only where Spring AI 1.1.1 requires WebFlux/WebClient internally (keep that scoped to `ai-core`).

## MVC + Virtual Thread Migration Notes
- Web stack: swap WebFlux for `spring-boot-starter-web`; controllers are blocking. Use `SseEmitter`/`ResponseBodyEmitter` for streaming.
- Backpressure/batching: replace `Flux#buffer` with bounded queues + batch drains (size/time thresholds) feeding handlers/sinks; make thresholds configurable.
- Workflow DSL: provide fluent/imperative chain (`workflow().source().handler().step().sink()`) with options for batch size/timeout, parallelism, and routing to keep Reactor-like ergonomics on blocking execution.
- Spring AI: stays on 1.1.1 for now; WebFlux dependency is isolated to `ai-core`. Long term, plan to adopt a non-reactive Spring AI release or fork to RestClient/RestTemplate.
- Concurrency: enable `spring.threads.virtual.enabled=true`; use virtual-thread executors by default, with bounded platform-thread pools for libraries unfriendly to virtual threads (e.g., Kafka/Ratis if needed).

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

## Language Rule
- All responses must be written in Korean.

## Planning & Execution Rule
- Before performing any task, the agent must create a clear and structured plan.
- If the plan contains multiple steps or is complex, the agent must write the full plan in the docs/PLAN.md.
- The plan must include a checklist so the user can review and track each step.
- During execution, the agent must update the checklist whenever a step is completed.
- If new steps become necessary during execution, the agent must add those steps to the existing plan document and update the checklist accordingly before continuing.

### Java Code Writing Rules (English)
- Use clear, intention-revealing names for classes, methods, variables, and fields to express their roles and purposes.
- Add a short class-level comment or Javadoc at the top of important classes to describe their responsibilities and roles within the architecture.
- Write Javadoc for public methods and key internal methods describing their behavior, parameters, return values, and possible exceptions.
- Add comments only where logic is not immediately obvious from the code itself, such as business rules, technical constraints, or non-trivial workarounds.
- Maintain consistent code formatting, including indentation, brace style, and spacing.
- Avoid magic numbers or hard-coded strings; extract them into well-named constants.
- When generating Java examples, prefer self-contained, compilable code snippets including necessary imports and declarations.
- When modifying or refactoring code, briefly summarize what was changed and why.
- When working within a framework (e.g., Source, Handler, PipelineStep, Sink), clearly describe the role each class plays within the framework.