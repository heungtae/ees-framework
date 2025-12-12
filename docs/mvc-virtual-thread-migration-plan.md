# Spring MVC + Virtual Thread Migration Plan

## Objectives
- Drop WebFlux/Reactor usage across modules and move to Spring MVC (servlet stack).
- Adopt virtual threads as the default execution model for web requests and internal background tasks.
- Preserve existing features (AI chat, workflow/pipeline, cluster metadata, MCP integration) with equivalent behavior and metrics.

## Current Reactive Footprint (inventory)
- HTTP/API: `AiChatController` now uses MVC + `SseEmitter` (no WebFlux handlers).
- AI core: Service interfaces are blocking; Spring AI usage still brings reactive types (`NoOpChatModel`, tests). `RestMcpClient` is blocking, but auto-config still builds a WebClient and `ai-core/pom.xml` keeps `spring-webflux`/Reactor.
- Workflow/pipeline: Core interfaces are blocking and `WorkflowEngine` now uses bounded-queue + batch drains; examples/tests are blocking.
- Metadata/messaging/cluster: Metadata-store API is blocking; cluster/raft services and membership tests still use `Flux`/`Mono`.
- Configuration: App is set to `web-application-type: servlet`; no virtual thread flags yet. WebFlux starter removed from server side, but `ai-core` still depends on it.

## Target Design Decisions
- Web stack: Use `spring-boot-starter-web` (Tomcat by default) with blocking controllers. SSE/streaming should use `SseEmitter`/`ResponseBodyEmitter` or chunked responses.
- HTTP clients: Replace WebClient with blocking `RestClient` (Spring 6+) or `RestTemplate` where needed.
- Concurrency: Enable virtual threads for request handling and async tasks; isolate blocking calls that are not virtual-thread-friendly (e.g., Kafka clients) with bounded executors.
- Backpressure: Replace Reactor backpressure with bounded queues/executors and timeouts per operation; explicitly design batching where `Flux#buffer` was used.
- Observability: Preserve existing metrics/logging; migrate Reactor-specific metrics/tests to synchronous equivalents.

## Build and Dependency Changes
- Root: Remove Reactor version property if unused; ensure BOM does not pull WebFlux transitively.
- Replace `spring-boot-starter-webflux` with `spring-boot-starter-web`; drop direct `spring-webflux` and `reactor-core` from module POMs (`ai-core/pom.xml`, `spring-boot-starter/pom.xml`, others).
- Remove `reactor-test` from tests; add `spring-boot-starter-test` (JUnit5/MockMvc) and, if needed, `awaitility` for async assertions.
- Add `spring-boot-starter-validation` if request validation is required post-migration.
- Spring AI(WebFlux 의존) 처리 옵션:
  - **옵션 A(단기)**: MVC 전환 후에도 `ai-core` 모듈에 한정해 WebFlux/Reactors를 구현 의존성으로 남겨 Spring AI 1.1.1을 내부에서만 사용. 서버 스타터는 `spring-boot-starter-web`만 포함.
  - **옵션 B(중장기)**: Spring AI를 비-reactive HTTP 클라이언트(RestClient/RestTemplate) 기반으로 포크/대체하거나, upstream 비-reactive 지원 버전(향후 1.2.x 등) 등장 시 교체.

## API Contract Refactors (core interfaces)
- Convert reactive signatures to blocking forms:
  - `AiAgentService.chat` -> `AiResponse`; define a streaming return type that fits MVC (e.g., `Iterator<AiResponse>` or callback-based emitter) instead of `Flux`.
  - `AiSessionService` -> blocking load/append operations.
  - Pipeline types: `Source.read` -> `Iterator< FxContext<T> >` or `Stream`; `PipelineStep.apply`/`Sink.write`/handlers -> blocking methods returning values or `void`.
  - `Workflow.start/stop` -> synchronous methods; if async execution is needed, wrap in virtual-thread executors.
  - Messaging/cluster watchers currently emitting `Flux` should move to listener/callback or blocking `Stream` APIs.
- Introduce temporary adapters (reactive -> blocking) only if staged rollout is required; plan to delete adapters once all call sites migrate.

## Module Workstreams
- **spring-boot-starter**: Swap WebFlux starter, update autoconfig to register MVC controllers, configure `TaskExecutor` for virtual threads, replace WebFlux-specific codecs. Update tests to MockMvc.
- **ai-core**: Rewrite `DefaultAiAgentService` to blocking calls; manage streaming responses with iterators/buffers usable by MVC SSE emitters. Replace WebClient in `RestMcpClient` with `RestClient`/`RestTemplate` and update retries/error mapping. Adjust tool callback wiring to remain synchronous.
- **workflow/pipeline/source/sink/handlers**: Redefine interfaces to blocking; rewrite `ReactorWorkflowEngine` into a synchronous engine that executes graph steps on virtual threads (per workflow or per node). Replace `Flux` chaining with imperative loops; keep cancellation hooks via cooperative flags.
- **Buffering/Batching (source→handler→sink)**: Where `Flux#buffer` batches records, introduce an explicit batching stage:
  - Accept records from `Source` into a bounded blocking queue (per workflow/run) with time/size thresholds.
  - A virtual-thread worker drains the queue into batches and invokes handler/pipeline/sink methods with `List<FxContext<T>>` or an adapter interface. (Engine side implemented with bounded queue + size/time drains; interfaces still single-item.)
  - Make batch size/timeout configurable (per workflow/pipeline) and enforce backpressure by rejecting or delaying producers when the queue is full.
- **metadata-store/messaging/cluster**: Replace `Flux` watchers with listener registration APIs (e.g., `subscribe(Consumer<Event>)`) and blocking fetch methods. Audit Kafka/Raft integrations to ensure thread management works with virtual threads or offload to platform threads where required.
- **application**: Change `spring.main.web-application-type` to `servlet`, configure virtual threads properties, adjust sample UI/API calls to new controller contract, and realign integration tests.
- **docs/examples**: Update docs and sample code to blocking API usage; remove Reactor-specific guidance.
- **Workflow DSL(개발 편의성 유지)**:
  - Fluent/imperative DSL 제공: `workflow("name").source(...).handler(...).step(...).sink(...)` 식으로 메서드 체이닝해 Reactor 체인과 비슷한 개발 경험을 제공.
  - 배치/백프레셔 옵션을 DSL 파라미터로 노출: `step(...).batchSize(100).batchTimeout(Duration.ofSeconds(1))`.
  - 병렬 처리 옵션: `step(...).parallelism(n)`을 명시하면 내부적으로 가상 스레드 풀에서 병렬 실행(입력 순서 보존 여부 선택).
  - 조건/분기 DSL: `route(condition, nextNode)` 형태로 분기 지정, Reactor의 `filter/switchOnFirst` 대체.
  - 실행기(엔진)는 가상 스레드 기반 blocking 실행이지만, DSL은 Reactor 스타일의 단문 체인과 옵션 설정만으로 동일한 표현력 확보.

## Virtual Thread Wiring
- Enable via configuration: `spring.threads.virtual.enabled=true` (Boot 3.3) and ensure server thread pool uses `VirtualThreadPerTaskExecutor`.
- Provide a `TaskExecutor` bean for internal tasks (`@Async`, workflow runs) using virtual threads; fall back to bounded platform-thread pools for libraries that block on carrier restrictions (check Kafka/Ratis).
- Revisit thread-locals and MDC propagation; replace Reactor context usage with explicit MDC setting in interceptors/filters.

## Testing and Rollout
- Update unit/integration tests to blocking style; replace Reactor `StepVerifier` with direct assertions or `Awaitility` for async completion.
- Regression checklist: chat sync/streaming endpoints, workflow execution, metadata scans/watches, cluster leadership/assignments, MCP REST client behaviors.
- Run `mvn clean install` and module-focused `mvn -pl <module> -am test` as contracts change.
- Document migration notes for app developers consuming the starter, including API signature changes and configuration flags.

## Risks / Open Questions
- Spring AI 1.1.1는 내부 WebFlux/WebClient를 사용하므로 완전한 Reactor 제거는 옵션 B(포크/대체)까지 진행해야 가능.
- Streaming behavior parity (SSE) after moving off `Flux` needs clear contract and buffering strategy.
- Kafka/Ratis clients may need dedicated platform-thread pools instead of virtual threads.
- Backpressure replacements (bounded queues vs. dropped messages) must be designed per data path.

## Execution Steps (phased)
1) **BOM/Dependency Prep**
   - Bump Spring Boot (e.g., 3.4.7), align Spring AI version; swap starters to `spring-boot-starter-web`, keep WebFlux scoped to `ai-core` only (옵션 A).
   - Remove Reactor version property if unused; drop `reactor-test` and add MockMvc/validation deps.
2) **API/Contract Refactor**
   - Convert interfaces to blocking: `Source/Handler/PipelineStep/Sink`, `Workflow`, `AiAgentService/AiSessionService`, metadata/messaging watchers.
   - Add batch-capable signatures where `Flux#buffer` existed (e.g., methods accepting `List<FxContext<T>>` or adapters).
3) **Engine/DSL Rewrite**
   - Replace `ReactorWorkflowEngine` with blocking/VT engine; implement batching/backpressure (bounded queues + size/time drains).
   - Introduce fluent DSL (`workflow().source().handler().step().sink()`) with options for batch size/timeout, parallelism, routing.
4) **AI Layer Adaptation**
   - Keep Spring AI 1.1.1 isolated: adapters from internal `Flux` to blocking/SSE emitters; replace WebClient uses with RestClient where possible, otherwise scope WebFlux to `ai-core`.
   - Define streaming contract for MVC (SseEmitter/ResponseBodyEmitter) and buffer strategy.
5) **Application Wiring**
   - Switch `spring.main.web-application-type` to `servlet`; enable `spring.threads.virtual.enabled=true`; wire virtual-thread executors and fallbacks for Kafka/Ratis if needed.
   - Update controllers to MVC, adjust configuration/properties, and sample UI paths.
6) **Testing & Rollout**
   - Rewrite tests to MockMvc/blocking; replace StepVerifier with direct assertions/Awaitility.
   - Module-by-module test runs, then `mvn clean install`; document migration notes and residual WebFlux scope.
