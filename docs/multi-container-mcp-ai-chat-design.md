# (보관) 멀티 컨테이너 + MCP 기반 설계 메모

> 현재 방향은 “EES 앱이 Web UI/API를 직접 노출하고 내부 서비스로 직접 제어”하는 방식이다. 최신 설계는 `docs/direct-control-web-ui-design.md`를 참고.

## 1) 배경과 목표

현재 `ees-framework` 저장소는 “샘플 애플리케이션(application 모듈)” 중심으로 한 프로세스에서 동작하는 형태에 가깝다. 하지만 요구사항은 다음과 같다.

- 실행 주체는 서로 다른 서버/컨테이너에서 구동된다.
- `ai-chat-web-application`에서 사용자가 입력한 요청(명령 포함)을 바탕으로 LLM이 Tool 호출을 결정한다.
- Tool 호출은 `mcp client`가 수행하며, 최종적으로 `ees-framework-application`이 “명령을 수신”하여 클러스터/워크플로/락/할당 등의 제어 작업을 수행해야 한다.

이 문서의 목표는:
- 런타임 토폴로지(컨테이너 분리)와 통신 경계를 명확히 하고,
- MCP Server API(제어 plane)의 계약을 정의하고,
- 현재 코드와의 갭(추가 개발 항목)을 정리해 이후 구현 계획으로 연결하는 것이다.

## 2) 현재 코드 기준 구성 요약(관찰)

### 2.1 AI Chat API/UI
- `spring-boot-starter/src/main/java/com/ees/ai/api/AiChatController.java`
  - `/api/ai/chat`, `/api/ai/chat/stream(SSE)`, `/api/ai/resources/*`(MCP 연동 리소스 조회) 제공 코드가 존재.
  - 단, 현재 `AiChatController`는 auto-configuration에서 `@Bean`/`@Import`로 등록되지 않아 “스타터를 의존하는 앱에서 자동 노출”이 보장되지 않는다.
- `application/src/main/resources/static/*`
  - 정적 UI(stub)로 AI Chat Web을 한 프로세스에서 제공하는 전제가 문서화되어 있음(`docs/ai-chat-web-plan.md`).

### 2.2 MCP 연동(클라이언트)
- `ai-core/src/main/java/com/ees/ai/mcp/RestMcpClient.java`
  - `/mcp/nodes`, `/mcp/topology`, `/mcp/workflows/*`, `/mcp/assignments`, `/mcp/locks` 등 “외부 MCP 서버”로 HTTP 호출하는 클라이언트만 존재.
- `spring-boot-starter/src/main/java/com/ees/ai/config/AiAutoConfiguration.java`
  - `ees.mcp.base-url` 설정이 있으면 `RestMcpClient`를 `McpClient`로 등록.
  - `McpToolBridge`를 통해 MCP 명령을 Spring AI `ToolCallback`로 노출.

### 2.3 MCP 서버(요청을 받는 쪽)
- 현재 트리에는 `/mcp/**` 요청을 처리하는 `@RestController`(또는 서블릿/핸들러) 구현이 없다.
- 따라서 “EES Framework Application이 MCP 명령을 받는다”는 요구를 충족하려면 MCP Server API를 신규 구현해야 한다.

### 2.4 워크플로 제어의 갭
- `workflow` 모듈은 `WorkflowRuntime.startAll()/stopAll()` 및 `Workflow.start()/stop()` 수준의 “런타임 제어” 중심.
- `RestMcpClient`가 가정하는 `start/pause/resume/cancel(executionId)`와 같은 “실행 인스턴스(execution) 단위 제어”는 현재 엔진 모델에 대응 개념이 없다.
- 즉, MCP API 계약을 확정하면서 “워크플로 제어 모델을 런타임 단위로 할지, 실행 인스턴스 단위로 확장할지”를 먼저 결정해야 한다.

## 3) 목표 런타임 토폴로지(권장)

### 3.1 컨테이너/서비스 분리

권장 구성(2개 컨테이너):

1) `ees-framework-application(+mcp-server)`
   - 역할: 제어 대상(클러스터/워크플로/락/할당/메타데이터)을 보유하고 실제로 상태를 변경한다.
   - 노출 API:
     - `MCP Server API` (예: `/mcp/**`) — 내부망 또는 제한된 접근만 허용
     - 운영용 `actuator` — 내부망

2) `ai-chat-web-application`
   - 역할: 사용자 요청 수신(UI/API), LLM 호출, Tool 호출 실행(= MCP Client), 결과를 사용자에게 반환
   - 노출 API:
     - 웹 UI(정적 파일 또는 별도 프론트) + `/api/ai/**`

대안(3개 컨테이너):
- `mcp-server`를 별도 컨테이너로 분리하고, 내부에서 `ees-framework-application`의 내부 API/메시징으로 브릿지하는 구성.
- 초기에는 복잡도 대비 효과가 낮으므로, 본 문서는 2개 컨테이너 구성을 기본으로 한다.

## 3.3 단일 서비스(통합 운영) 가능성 및 “프로퍼티 기반 토글” 검토

요구한 멀티 컨테이너(분리) 외에, 운영 단순화를 위해 다음 2가지 형태의 “통합”도 현실적으로 가능하다.

### A) 완전 통합(단일 컨테이너 1개)
- 한 프로세스가 아래를 모두 제공:
  - AI Chat Web(UI + `/api/ai/**`)
  - MCP Server(`/mcp/**`) + 실제 제어 대상(클러스터/워크플로/락/할당)
- 가능 여부: **가능(설계/구현 보완 필요)**
- 핵심 보완점:
  - 현재 repo에는 MCP Server가 없으므로 `/mcp/**` 서버 구현이 필요.
  - 통합 모드에서는 AI 쪽 Tool 호출이 “원격 HTTP MCP”가 아니라 “로컬 호출”로 떨어지는 것이 자연스럽다.
    - 즉, `McpClient`에 `local` 구현(예: `LocalMcpClient`)이 필요하고, 프로퍼티로 `remote/local`을 선택해야 한다.
  - 보안 관점에서 `/mcp/**`는 반드시 static token 인증(확정) + (권장) 내부망/리버스 프록시 ACL로 보호해야 한다.

### B) 단일 아티팩트(동일 JAR) + 분리 배포(컨테이너 2개, 프로파일만 다름)
- “빌드 산출물은 하나”지만, 실행 시 프로퍼티/프로파일로 역할을 나눠서 **2개 컨테이너로 띄우는 방식**.
  - 컨테이너 1: `ees` 프로파일 → MCP Server만 활성화, AI Chat 비활성화
  - 컨테이너 2: `ai-chat` 프로파일 → AI Chat만 활성화, MCP Server 비활성화, MCP Client는 remote로 EES 컨테이너 호출
- 가능 여부: **가능(조건부 오토컨피그/컨트롤러 토글이 전제)**
- 장점:
  - 운영/배포는 여전히 분리(스케일링/보안 경계 유지)하면서, 빌드·릴리스 관리가 단순해진다.

### 프로퍼티로 통합/분리를 “가능한 수준”까지 만들기 위한 조건
현재 코드 상태에서는 아래 토글 키가 존재하지 않으므로, 구현이 필요하다(“가능”은 하지만, 작업이 선행되어야 함).

권장 토글 키(제안):
- `ees.ai.enabled` (기본 `true`): AI 관련 Bean(모델/에이전트/툴) 전체 on/off
- `ees.ai.web.enabled` (기본 `false`): `/api/ai/**` 컨트롤러 및 UI 제공 on/off
- `ees.mcp.server.enabled` (기본 `false`): `/mcp/**` 서버 on/off (신규 모듈에서 제공)
- `ees.mcp.client.mode` = `remote|local` (기본 `remote`)
  - `remote`: `ees.mcp.base-url` 필수, `RestMcpClient` 사용
  - `local`: 동일 프로세스 내 MCP 서비스(서버 서비스 계층)로 직접 위임하는 `LocalMcpClient` 사용
- `ees.mcp.server.auth-token` (필수): static token 값

권장 프로파일 예시(개념):
- `application-ees.yaml`
  - `ees.ai.web.enabled=false`
  - `ees.mcp.server.enabled=true`
  - `ees.mcp.client.mode=local` (선택: EES 내부에서 Tool 호출이 필요 없다면 client 자체를 disable 하는 옵션도 고려)
- `application-ai-chat.yaml`
  - `ees.ai.web.enabled=true`
  - `ees.mcp.server.enabled=false`
  - `ees.mcp.client.mode=remote`
  - `ees.mcp.base-url=http://ees-app:8081`

주의:
- 현재 `ees-spring-boot-starter`가 `ees-ai-core`를 항상 포함하므로(의존성 레벨), “완전한 경량 분리(LLM 라이브러리 자체를 제외)”까지는 프로퍼티만으로는 어렵다.
  - 정말로 바이너리/의존성까지 분리하려면 “AI 기능을 별도 starter로 분리”하는 구조 변경이 필요하다.
  - 다만 “엔드포인트/기능 노출을 on/off 하는 수준”은 프로퍼티 기반으로 충분히 가능하다.
- 또한 현재 코드 기준으로 `AiChatController`는 auto-config로 등록되지 않는다.
  - 따라서 `ees.ai.web.enabled=true`를 실제로 의미 있게 만들려면, (1) 신규 `ai-chat-web` 앱에 컨트롤러를 둔다, 또는 (2) starter에서 조건부로 `AiChatController`를 `@Bean`/`@Import`로 등록하는 방식 중 하나를 구현해야 한다.

### 3.2 네트워크/보안 경계
- `ai-chat-web-application` → `ees-framework-application` 호출은 내부 DNS(예: `http://ees-app:8081`)로 통신.
- MCP Server API는 반드시 인증을 요구:
  - 1차: Bearer 토큰(Static token) + 방화벽/내부망
  - 2차(확장): mTLS 또는 OAuth2 client credentials
- 감사(audit) 로그는 최소한 다음을 포함:
  - `userId`, `sessionId`, `toolName`, `args`, `result/err`, `requestId`, `remoteIp`

## 4) MCP Server API 계약(초안)

> 주의: 아래는 “현재 `RestMcpClient`가 기대하는 경로”와 “프레임워크가 실제로 제공할 수 있는 제어 모델”을 동시에 고려한 초안이다. 구현 전에 `5장`의 결정 사항을 먼저 확정한다.

### 4.1 공통
- Base path: `/mcp`
- Content-Type: `application/json`
- 인증:
  - `Authorization: Bearer <token>`
- 오류 표준:
  - `400` 입력 검증 실패
  - `401/403` 인증/인가 실패
  - `404` 대상 없음
  - `409` 상태 충돌(예: 이미 실행 중)
  - `500` 내부 오류

### 4.2 클러스터/노드
- `GET /mcp/nodes`
  - 목적: 멤버십 뷰 조회(`ClusterMembershipService.view()`)
- `GET /mcp/topology`
  - 목적: 멤버십 + 할당/리더 정보 등 토폴로지 요약
  - 확장: `GET /mcp/topology/stream` (SSE)로 토폴로지 이벤트 스트림 제공

### 4.3 할당(Assignment)
- `POST /mcp/assignments`
  - 입력(예): `{ "group": "...", "partition": 0, "kind": "...", "key": "...", "appId": "..." }`
  - 처리: `AssignmentService.assignKey(...)`
  - 참고: 현재 `AssignmentService`는 `KeyAssignmentSource`를 요구하므로, MCP 호출은 `KeyAssignmentSource.MCP` 같은 값을 신규로 추가하거나, 서버에서 고정값을 사용하도록 어댑터 계층이 필요하다.

### 4.4 락(Distributed lock)
- `POST /mcp/locks`
  - 입력(예): `{ "name": "...", "ttlSeconds": 30 }`
  - 처리: `DistributedLockService.tryAcquire(...)`
  - 설계 포인트: `ownerNodeId`를 무엇으로 둘지(예: `mcp:<clientId>` 또는 `ai-chat:<userId>`) 결정 필요
- `DELETE /mcp/locks/{name}`
  - 처리: `DistributedLockService.release(...)`

### 4.5 워크플로
현 엔진 모델과의 정합성 때문에 아래 두 방향 중 하나를 선택해야 한다.

#### 옵션 A) “런타임 제어형” MCP (MVP에 적합)
- 의미:
  - `workflowId`는 “등록된 워크플로 이름”
  - `executionId`는 사용하지 않거나 `workflowId`와 동일 취급
- API 예:
  - `POST /mcp/workflows/{workflowId}/start` → `Workflow.start()`
  - `POST /mcp/workflows/{workflowId}/pause` → (실질적으로) `Workflow.stop()`
  - `POST /mcp/workflows/{workflowId}/resume` → `Workflow.start()`
  - `POST /mcp/workflows/{workflowId}/cancel` → `Workflow.stop()` (idempotent)
  - `GET /mcp/workflows/{workflowId}` → 상태(시작 여부 등) 반환(새 DTO 필요)
- 장점: 현재 엔진 기반으로 빠르게 end-to-end를 만든다.
- 단점: “실행 인스턴스” 개념/파라미터 기반 단발 실행을 제공하지 못한다.

#### 옵션 B) “실행 인스턴스 제어형” MCP (장기 정합성)
- 의미:
  - `startWorkflow`는 파라미터를 받아 “새 실행 인스턴스”를 만들고 `executionId`를 반환
  - `pause/resume/cancel/getState`는 `executionId` 기준으로 동작
- 필요 개발:
  - `WorkflowExecution` 모델/상태 저장소/이벤트
  - 실행 취소/일시정지/재개를 위한 엔진 확장(또는 작업 큐/스케줄러 계층)
- 장점: `RestMcpClient`/툴 목록과 의미가 일치하고, UI에서 제어가 명확하다.
- 단점: 구현 범위가 커서 1차 MVP로는 과할 수 있다.

## 5) 결정이 필요한 사항(우선순위)

1) 워크플로 제어 모델 선택: **옵션 A(런타임 제어형, MVP)** 로 확정
2) MCP Server API를 어느 모듈에 둘지: **신규 모듈로 분리**(예: `ees-mcp-server-starter`)로 확정
3) `ai-chat-web-application`을 이 repo에서 제공할지(샘플) vs 다운스트림에서 제공할지: **결정 필요(아래 설명 참고)**
4) 인증 방식: **Static token(1차)** 로 확정 (OAuth2/mTLS는 2차 확장)

### 5.1 결정(1): 워크플로 제어 모델 = 옵션 A(런타임 제어형)
- 1차 목표는 “멀티 컨테이너에서 end-to-end로 명령이 전달되고, EES 앱이 실제로 제어를 수행한다”를 빠르게 검증하는 것이다.
- 따라서 `start/pause/resume/cancel/getState`는 “executionId”가 아니라 “등록된 워크플로 이름(workflowId)” 중심으로 동작하도록 정의한다.

### 5.2 결정(2): MCP Server는 신규 모듈로 분리
- 목적: `spring-boot-starter`의 책임(프레임워크 + 기본 오토컨피그)과 “제어 plane API(MCP Server)”를 분리해 의존성과 노출면(attack surface)을 최소화한다.
- 제안 모듈(예): `mcp-server-starter`
  - artifactId: `ees-mcp-server-starter`
  - 역할: `/mcp/**` 컨트롤러 + 서비스 어댑터 + static token 인증 + 감사 로그 + (선택) actuator health
  - 사용: `ees-framework-application` 컨테이너가 이 모듈을 의존하여 MCP Server API를 노출

### 5.3 결정 필요(3): AI Chat Web App를 이 repo에 둘지 vs 다운스트림으로 둘지(설명)
이 항목은 “기능”이 아니라 “운영/소유/배포” 선택이다. 무엇이 정답이라기보다, 팀 운영 방식에 따라 트레이드오프가 갈린다.

#### 옵션 3-A) 이 repo에 `ai-chat-web-application`(샘플/레퍼런스) 모듈을 둔다
- 장점
  - 프레임워크 변경과 UI/AI 연동 변경을 **같은 PR**에서 검증 가능(통합 테스트/로컬 개발이 쉬움).
  - `McpClient`/`AiAutoConfiguration` 변경 시 레퍼런스 앱으로 즉시 회귀 확인 가능.
  - “최소 예제”로 문서/온보딩이 쉬움.
- 단점
  - 제품/서비스 특화 요구(인증, UI, 배포 파이프라인)가 프레임워크 repo에 섞일 위험.
  - LLM 키/보안 설정/운영 설정이 샘플과 섞여 관리가 복잡해질 수 있음(특히 실서비스로 확장 시).

#### 옵션 3-B) `ai-chat-web-application`을 다운스트림(별도 repo/프로젝트)으로 둔다
- 장점
  - 프레임워크는 “라이브러리/스타터”로 유지되고, 제품/서비스 앱은 별도로 독립 배포 가능(소유권 분리).
  - 서비스별 UI/인증/권한/감사/로깅 요구를 자유롭게 진화시킬 수 있음.
- 단점
  - 프레임워크 변경 → 서비스 앱 호환성 검증이 느려질 수 있음(버전 관리/릴리스 정책이 중요).
  - 레퍼런스/샘플이 없으면 신규 사용자 온보딩이 어려워질 수 있음.

#### 추천(실무형)
- 초기(MVP)는 **옵션 3-A**로 “샘플/레퍼런스”를 repo 내에 두되,
- 구조적으로는 언제든 **옵션 3-B**로 분리 가능한 형태(의존은 `ees-spring-boot-starter`만, 환경설정은 외부화)로 만든다.

## 6) 추가 개발이 필요한 내용(요약)

### 6.1 MCP Server 구현(필수)
- `@RestController` 기반 `/mcp/**` API 신규 구현
- 인증/인가 필터(또는 `spring-security` 도입) + 감사 로그
- `AssignmentService`, `DistributedLockService`, `ClusterMembershipService`, `LeaderElectionService`, `WorkflowRuntime`(또는 신규 ControlService)로의 어댑팅 계층

### 6.2 AI Chat Web Application 분리(필수)
- “별도 컨테이너”로 패키징되는 Spring Boot 앱(새 모듈) 추가 또는 기존 `application` 모듈 역할 전환
- `/api/ai/**` 컨트롤러가 실제로 등록되도록 auto-config 정리
  - 예: `AiAutoConfiguration`에서 `AiChatController`를 `@Bean`으로 노출하거나 `@Import`

### 6.3 설정 체계 분리(필수)
- `ees-framework-application`:
  - `ees.mcp.server.*` (port/base-path/auth-token/allowed-tools 등, static token 기반)
- `ai-chat-web-application`:
  - `ees.mcp.base-url` (EES 앱의 MCP Server 주소)
  - `spring.ai.*` (LLM 공급자 키)

### 6.4 운영/관측(권장)
- `actuator` health:
  - EES 앱: 클러스터/락/할당/워크플로 런타임
  - AI Chat 앱: LLM readiness + MCP connectivity
- rate-limit / audit 로그를 “AI Chat 앱”과 “EES 앱” 양쪽에 둔다(이중 방어)

## 7) 요청-처리 시퀀스(텍스트)

1) 사용자가 `ai-chat-web-application` UI에서 “명령”을 입력한다.
2) AI Chat API(`/api/ai/chat` 또는 `/api/ai/chat/stream`)가 LLM 호출을 수행한다.
3) LLM이 tool-call을 결정하면, `McpToolBridge` → `McpClient(RestMcpClient)`가 `ees-framework-application`의 `/mcp/**`로 HTTP 요청을 보낸다.
4) `ees-framework-application`의 MCP Server Controller가 인증/인가 후 서비스 계층으로 위임하여 실제 상태 변경/조회 작업을 수행한다.
5) 결과가 AI Chat 앱으로 돌아오고, 최종 응답이 사용자에게 렌더링된다(SSE인 경우 스트리밍으로 전달).
