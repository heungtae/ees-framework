# AI Agent Integration Design (Spring AI + MCP)

## Goals
- Add an AI agent that can control the framework via MCP (Meta Control Protocol).
- Provide a Web UI “AI Chat Console” for user commands and streaming responses.
- Allow workflows to embed AI Agent steps (decision/support actions).
- Base LLM integration on Spring AI for provider portability and tool calling.

## Architecture Overview
- **ai-core (new)**: Spring AI `ChatModel`/`StreamingChatModel`, Tool Registry, session/history service.
- **MCP Bridge**: Tools that translate Spring AI function-calling to internal MCP commands (cluster/workflow/metadata).
- **Workflow integration**: `AiAgentStep` in workflow DSL uses Spring AI + tool whitelist to act during workflow execution.
- **Web UI/API**: WebFlux REST + WebSocket/SSE endpoints to drive chat; UI consumes streaming tokens and structured tool results.
- **Security/ops**: Tool whitelist per role, rate limits, audit log of MCP commands and agent actions.

## Modules & Placement
- `ai-core` (could live in `spring-boot-starter` or a new module):
  - Beans: `ChatModel`, `StreamingChatModel`, `AiAgentService`, `AiToolRegistry`, `AiSessionService`.
  - Config props: `ees.ai.model`, `ees.ai.streaming-enabled`, `ees.ai.tools.allowed[]`, `ees.ai.history.store`, `ees.ai.rate-limit.*`.
  - Depends on Spring AI starter (`spring-ai-spring-boot-starter`).
- `cluster`/`workflow`/`metadata-store`:
  - MCP Server endpoints expose control: `listNodes`, `startWorkflow`, `pause/resume/cancel`, `describeTopology`, `assignKey`, `lock/release`, `getWorkflowState`, etc.
  - Event streams for topology/assignment/workflow state changes.
- `spring-boot-starter`:
  - Auto-config for AI beans, MCP server/client beans, REST/WebSocket controllers for chat.
  - Health/metrics for agent latency, tool calls, error rates.
- `application`:
  - Sample Web UI (React/Vite or minimal WebFlux/Thymeleaf) for the AI Chat Console.

## Spring AI Usage
- **Model abstraction**: Use `ChatModel` for sync responses; `StreamingChatModel` for token streaming.
- **Tools / Function calling**: Register `ToolCallback` functions that call MCP commands (and internal utilities).
- **Prompting**: Use `PromptTemplate` and `Message` history; inject workflow/cluster context as system messages.
- **History**: Pluggable store (in-memory/metadata-store/Redis) keyed by `sessionId`.

## MCP Tool Bridge (example methods)
- `startWorkflow(workflowId, params?)`
- `pauseWorkflow(executionId)`
- `resumeWorkflow(executionId)`
- `cancelWorkflow(executionId)`
- `describeTopology()`
- `listNodes()`
- `assignKey(group, partition, key, appId)`
- `lock(name, ttl)`
- `releaseLock(name)`
- `getWorkflowState(executionId)`
- Stream: workflow state changes, cluster topology events, assignment changes.

## Workflow DSL: AiAgentStep (planned)
Attributes:
- `model`: model id (defaults to `ees.ai.model`).
- `toolsAllowed`: subset of tools/functions permitted in this step.
- `promptTemplate`: base prompt or instructions.
- `timeout`, `maxTokens`, `temperature`, `stop`.
- `historyWindow`: how much chat context to include.
- Output handling: map LLM response to workflow variables or downstream steps; error/retry policy.

Execution flow:
1) Workflow engine invokes `AiAgentStep` with context (inputs, prior state).
2) AiAgent uses Spring AI `ChatModel` + allowed tools to decide/act (may call MCP).
3) Result persisted to workflow state and emitted to observability/events.

## Web UI / API
- Endpoints:
  - `POST /api/ai/chat` (sync) → `ChatModel`.
  - `POST /api/ai/chat/stream` (SSE or WebSocket) → `StreamingChatModel`.
  - `GET /api/ai/sessions/{id}` for history.
- Payload: `{sessionId, userId, messages[], toolsAllowed?, contextHints?}`.
- UI features:
  - Chat panel with streaming tokens.
  - MCP resource browser (workflows, nodes, topology).
  - Structured tool results rendering (tables/cards).
  - Error/approval prompts for dangerous commands.

## Security & Governance
- Tool whitelist per role/user; block destructive MCP commands unless allowed.
- Rate limiting per user/session.
- Audit log of: user prompt, model response, tool calls (MCP command, args, status).
- Secrets via Spring AI provider configs (`spring.ai.*`); no secrets in prompts.

## Observability
- Metrics: latency (prompt→response), tool call count/error rate, tokens in/out, MCP command latency.
- Health: agent readiness, MCP connectivity, provider availability.
- Traces: span around chat request, tool calls, workflow step executions.

## Implementation Plan (phased)
1) **Spec**: Finalize MCP methods/schemas and AiAgentStep DSL (props, outputs).
2) **AI core**: Add Spring AI starter, define beans (ChatModel/StreamingChatModel, Tool registry, session service).
3) **MCP tools**: Implement ToolCallbacks that invoke MCP client; add audit hooks.
4) **API/UI**: Expose REST/WebSocket endpoints; stub React/Vite console consuming streaming responses.
5) **Workflow**: Add `AiAgentStep` execution path with tool whitelist and context injection.
6) **Security/obs**: Apply whitelists, rate limits, metrics/health; wire audit logging.
