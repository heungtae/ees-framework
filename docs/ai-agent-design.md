# AI 에이전트 통합 설계 (Spring AI + MCP)

## 목표
- MCP(Meta Control Protocol)를 통해 프레임워크를 제어하는 AI 에이전트 추가.
- 사용자 명령과 스트리밍 응답을 제공하는 웹 UI “AI Chat Console” 제공.
- 워크플로 내에 AI Agent 스텝(의사결정/지원 액션) 삽입.
- LLM 연동은 Spring AI 기반으로 공급자 이식성과 툴 콜링 확보.

## 아키텍처 개요
- **ai-core(신규)**: Spring AI `ChatModel`/`StreamingChatModel`, 툴 레지스트리, 세션/히스토리 서비스.
- **MCP 브리지**: Spring AI 함수 호출을 내부 MCP 명령(클러스터/워크플로/메타데이터)로 변환하는 툴.
- **워크플로 연동**: 워크플로 DSL의 `AiAgentStep`이 Spring AI와 툴 화이트리스트를 사용해 실행 중 액션 수행.
- **Web UI/API**: WebFlux REST + WebSocket/SSE 엔드포인트로 채팅을 구동; UI는 스트리밍 토큰과 구조화된 툴 결과를 소비.
- **보안/운영**: 역할별 툴 화이트리스트, 레이트 리밋, MCP 명령/에이전트 액션 감사 로그.

## 모듈 및 배치
- `ai-core`(새 모듈이거나 `spring-boot-starter` 내부):
  - 빈: `ChatModel`, `StreamingChatModel`, `AiAgentService`, `AiToolRegistry`, `AiSessionService`.
  - 설정: `ees.ai.model`, `ees.ai.streaming-enabled`, `ees.ai.tools.allowed[]`, `ees.ai.history.store`, `ees.ai.rate-limit.*`.
  - 의존: Spring AI 스타터(`spring-ai-spring-boot-starter`).
- `cluster`/`workflow`/`metadata-store`:
  - MCP 서버 엔드포인트: `listNodes`, `startWorkflow`, `pause/resume/cancel`, `describeTopology`, `assignKey`, `lock/release`, `getWorkflowState` 등.
  - 토폴로지/할당/워크플로 상태 변경 이벤트 스트림 제공.
- `spring-boot-starter`:
  - AI 빈, MCP 서버/클라이언트 빈, 채팅용 REST/WebSocket 컨트롤러 자동 설정.
  - 에이전트 지연, 툴 호출, 오류율 등의 헬스/메트릭 제공.
- `application`:
  - AI Chat Console 샘플 Web UI(React/Vite 또는 최소 WebFlux/Thymeleaf).

## Spring AI 활용
- **모델 추상화**: 동기 응답은 `ChatModel`, 토큰 스트리밍은 `StreamingChatModel`.
- **툴/함수 호출**: MCP 명령(및 내부 유틸)을 호출하는 `ToolCallback` 등록.
- **프롬프트**: `PromptTemplate`과 `Message` 히스토리 사용; 워크플로/클러스터 컨텍스트를 시스템 메시지로 주입.
- **히스토리**: `sessionId`로 키잉되는 플러그형 저장소(인메모리/metadata-store/Redis).

## MCP 툴 브리지 (예시 메서드)
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
- 스트림: 워크플로 상태 변화, 클러스터 토폴로지 이벤트, 할당 변경.

## 워크플로 DSL: AiAgentStep (계획)
속성:
- `model`: 모델 ID(`ees.ai.model` 기본값).
- `toolsAllowed`: 이 스텝에서 허용된 툴/함수 하위집합.
- `promptTemplate`: 기본 프롬프트 또는 지침.
- `timeout`, `maxTokens`, `temperature`, `stop`.
- `historyWindow`: 포함할 대화 컨텍스트 길이.
- 출력 처리: LLM 응답을 워크플로 변수나 다운스트림 스텝에 매핑; 오류/재시도 정책.

실행 흐름:
1) 워크플로 엔진이 컨텍스트(입력, 이전 상태)로 `AiAgentStep` 호출.
2) AiAgent가 Spring AI `ChatModel` + 허용된 툴로 판단/액션 수행(MCP 호출 가능).
3) 결과를 워크플로 상태에 저장하고 관측/이벤트로 전파.

## Web UI / API
- 엔드포인트:
  - `POST /api/ai/chat` (동기) → `ChatModel`.
  - `POST /api/ai/chat/stream` (SSE 또는 WebSocket) → `StreamingChatModel`.
  - `GET /api/ai/sessions/{id}`: 히스토리 조회.
- 페이로드: `{sessionId, userId, messages[], toolsAllowed?, contextHints?}`.
- UI 기능:
  - 스트리밍 토큰을 보여주는 채팅 패널.
  - MCP 리소스 브라우저(워크플로, 노드, 토폴로지).
  - 구조화된 툴 결과 렌더링(테이블/카드).
  - 위험 명령에 대한 오류/승인 프롬프트.

## 사전 정의 명령(커맨드 팔레트)
- 목적: AI Chat UI에 미리 정의된 MCP 제어 명령을 노출하고, 사용자가 선택→파라미터 입력→실행하도록 안전/일관성 확보.
- 명령 카테고리 예시:
  - 워크플로: `startWorkflow(id, params)`, `pause/resume/cancel(executionId)`, `getWorkflowState(executionId)`, `listWorkflows()`.
  - 클러스터/노드: `listNodes()`, `describeTopology()`, `setClusterMode(mode)`, `drainNode(nodeId)`, `rebalanceAssignments()`.
  - 할당: `assignKey(group, partition, key, appId)`, `unassignKey(key)`, `describeAssignments(group?)`.
  - 락: `lock(name, ttl)`, `releaseLock(name)`, `listLocks()`.
  - 리더십: `forceReelection(mode?)`, `getLeaderInfo(mode?)`.
  - 모니터링: `tailEvents(type, since?)`, `getMetrics(scope?)`.
- UI 동작:
  - 커맨드 팔레트/검색창에서 명령 선택 후 파라미터 폼 표시(기본값/검증 포함).
  - 취소/재선출/락 해제 등 위험 명령은 추가 확인 단계 제공.
  - 실행 결과를 패널에 표시(성공/오류 + 테이블/카드 형태 데이터).
  - 최근 실행/즐겨찾기/단축키로 재실행 지원.
- 보안/권한:
  - 역할별 화이트리스트로 노출 및 실행 허용 범위 제어.
  - 파라미터 검증(범위, TTL 제한 등)과 기본 안전값 제공.
- 실행 흐름: UI 선택 → 파라미터 검증 → MCP 툴 호출 → 결과/이벤트 스트림 렌더링.

## 보안 및 거버넌스
- 역할/사용자별 툴 화이트리스트; 허용되지 않은 파괴적 MCP 명령은 차단.
- 사용자/세션 단위 레이트 리밋.
- 감사 로그: 사용자 프롬프트, 모델 응답, 툴 호출(MCP 명령, 인자, 상태).
- 비밀은 Spring AI 공급자 설정(`spring.ai.*`)으로 관리; 프롬프트에 비밀 삽입 금지.

## 관측성
- 메트릭: 지연(프롬프트→응답), 툴 호출 횟수/오류율, 토큰 입출력, MCP 명령 지연.
- 헬스: 에이전트 준비 상태, MCP 연결성, 공급자 가용성.
- 트레이스: 채팅 요청, 툴 호출, 워크플로 스텝 실행을 감싸는 스팬.

## 구현 계획(단계별)
1) **사양**: MCP 메서드/스키마와 AiAgentStep DSL(속성, 출력) 확정.
2) **AI 코어**: Spring AI 스타터 추가, 빈 정의(ChatModel/StreamingChatModel, 툴 레지스트리, 세션 서비스).
3) **MCP 툴**: MCP 클라이언트를 호출하는 ToolCallback 구현; 감사 훅 추가.
4) **API/UI**: REST/WebSocket 엔드포인트 노출; 스트리밍 응답을 소비하는 React/Vite 콘솔 스텁.
5) **워크플로**: `AiAgentStep` 실행 경로에 툴 화이트리스트와 컨텍스트 주입 추가.
6) **보안/관측**: 화이트리스트, 레이트 리밋, 메트릭/헬스 적용; 감사 로깅 연결.

## 실행 상태
- [x] Spring AI 의존성 추가 및 AI 코어 스켈레톤 빈(AiAgentService/Session/Tool 레지스트리) 등록
- [x] 워크플로/클러스터 부트 시 필요한 기본 설정(클러스터 포트 기본값, 워크플로 자동 설정 임포트) 복원
- [x] 루트 빌드/테스트 통과 확인(`mvn clean install`)
- [x] MCP 툴 브리지 구현 및 툴 화이트리스트 적용 (Spring AI ToolCallback + MCP 호출/감사 스텁 포함)
- [x] REST/SSE API + Chat UI 스텁 노출 및 기본 검증 추가
- [x] `AiAgentStep` 워크플로 통합(파이프라인 스텝) 및 기본 테스트 추가

## 다음 단계
- MCP transport를 실제 스펙에 맞게 연결하고 오류 매핑/인증/타임아웃/재시도 정책 적용.
- AiAgentService 모델 호출에 toolsAllowed 가드를 모델 옵션/프롬프트에 반영하고 Spring AI ToolCallback 연동 강화를 지속.
- 레이트 리밋, 메트릭, 감사 로그 등 관측/보안 훅 연동.
- REST/SSE API와 Chat UI에 파라미터 검증, 위험 명령 승인, MCP 리소스 브라우저 등을 추가.
- 워크플로 DSL 샘플 그래프/테스트에 AiAgentStep을 포함하고 에러/재시도/화이트리스트 처리 강화.
- 히스토리 저장소 플러그인(metadata-store/Redis 등) 추가.

## 남은 과제 체크리스트
- [x] MCP 클라이언트/ToolCallback를 실제 MCP transport와 연결하고 오류 매핑·인증·재시도 적용
- [x] AiAgentService 모델 호출 시 toolsAllowed/프롬프트 가드 적용 및 Spring AI ToolCallback 연동 강화
- [x] 레이트 리밋, 메트릭, 감사 로그 등의 관측·보안 훅 구현
- [x] REST/SSE API 및 Chat UI에 파라미터 검증·오류/승인 프롬프트·MCP 리소스 브라우저 추가
- [x] 워크플로 DSL 샘플 그래프/테스트에 AiAgentStep 통합 및 에러/재시도/툴 화이트리스트 처리
- [x] 히스토리 저장소 플러그인(metadata-store/Redis 등) 추가
