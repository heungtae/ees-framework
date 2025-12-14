# 설계: AI Web UI “듀얼 제공”(Standalone 앱 + EES 앱 Embedded) + 직접 제어(Control API)

## 1) 제안 요약

요구사항:
- 사용자는 **별도 `ai-web` 애플리케이션**에 접속할 수도 있어야 하고(Standalone),
- **`ees-framework-application` 자체**에서도 동일한 Web UI를 직접 제공할 수 있어야 한다(Embedded).

또한 제어는 “MCP 서버 호출”이 아니라, EES가 제공하는 **Control API/Façade**를 통해 수행한다.

핵심 의도:
- 운영 단위를 단순화(서비스/컨테이너 수 감소)
- 네트워크 홉 감소(지연/장애 지점 감소)
- 타입 안정성/테스트 용이성 향상(HTTP 계약 대신 서비스 호출)

## 2) “컨트롤러를 직접 호출” vs 권장 구조

같은 프로세스 안에서 “A 컨트롤러가 B 컨트롤러를 직접 호출”하는 패턴은 권장하지 않는다.
컨트롤러는 HTTP 경계(직렬화/검증/인증/응답코드)를 담당하고, 비즈니스 제어는 서비스 계층이 담당해야 한다.

권장 구조:
- Web UI/외부 호출 → `@RestController` → `ControlFacade`(서비스) → (Cluster/Workflow/Lock/Assignment 서비스들)
- AI Tool 호출(Spring AI ToolCallback) → `ControlFacade`(서비스) → (동일)

이렇게 하면:
- HTTP/JSON 변환 비용을 줄이고
- 동일한 검증/인가 정책을 한 곳(Facade/Service)에서 재사용하며
- 컨트롤러는 얇게 유지할 수 있다.

## 3) 런타임 구성(단일 서비스 기본)

## 3) 런타임 구성: 2가지 제공 모드

### 3.1 Embedded 모드(= EES 앱이 Web UI도 제공)
- 단일 애플리케이션: `ees-framework-application`
  - Web UI(정적 리소스)
  - AI Chat API: `/api/ai/**`
  - Control API: `/api/control/**` (또는 `/api/admin/**`)
  - Control 호출 방식: **local** (동일 프로세스 `ControlFacade` 직접 호출)

기본 운영 모드(결정):
- **기본은 Embedded**로 한다.
- Standalone(`ai-web-application`)은 필요 시 추가로 제공한다(같은 UI/기능을 원격 제어로 사용).

### 3.2 Standalone 모드(= 별도 ai-web 앱으로 제공)
- 2개 애플리케이션:
  1) `ees-framework-application`
     - Control API: `/api/control/**` (static token 보호)
  2) `ai-web-application`
     - Web UI + AI Chat API: `/api/ai/**`
     - Control 호출 방식: **remote** (`ees-framework-application`의 Control API를 HTTP로 호출)

핵심은 “UI/AI는 어디서 돌든 동일”하게 하고, 제어 호출만 `local/remote`로 교체하는 것이다.

## 4) 제어 API(초안)

> 워크플로 제어는 MVP로 “옵션 A(런타임 제어형)”을 유지한다.

공통:
- Base path(예): `/api/control`
- 인증: `Authorization: Bearer <static-token>` (리버스 프록시/내부망 권장)
- 감사 로그: `userId/sessionId/toolName/args/result/remoteIp/requestId`

예시 엔드포인트:
- `GET /api/control/nodes` : 멤버십 뷰
- `GET /api/control/topology` : 토폴로지 요약
- `POST /api/control/assignments` : 키 할당
- `POST /api/control/locks` / `DELETE /api/control/locks/{name}` : 락 획득/해제
- `POST /api/control/workflows/{workflowId}/start|pause|resume|cancel`
- `GET /api/control/workflows/{workflowId}` : 상태(시작 여부 등)

## 5) 프로퍼티 토글(가능 범위)

“Standalone + Embedded”를 동시에 지원하려면, 아래 2축을 프로퍼티로 토글할 수 있어야 한다.
- (A) Web UI/AI Chat 엔드포인트를 이 프로세스에서 노출할지 여부
- (B) Control 호출을 local로 할지 remote로 할지 여부

권장 키(제안):
- `ees.ai.web.enabled` (boolean): Web UI + `/api/ai/**` 노출 on/off
- `ees.control.web.enabled` (boolean): `/api/control/**` 노출 on/off
- `ees.control.mode` (`local|remote`): AI Tool/서비스가 제어를 수행하는 방식
  - `local`: `ControlFacade` 직접 호출(Embedded 권장)
  - `remote`: `ControlHttpClient`로 `ees.control.base-url` 호출(Standalone 권장)
- `ees.control.base-url` (remote일 때 필수): 예) `http://ees-app:8081`
- `ees.control.auth-token` (필수): static token 값

Embedded 기본값(권장):
- `ees.ai.web.enabled=true`
- `ees.control.web.enabled=true`
- `ees.control.mode=local`

주의:
- 의존성(LLM 라이브러리 등)까지 완전히 분리하려면 모듈/스타터 레벨 구조 변경이 필요하다.
- 현재 코드 기준으로 `AiChatController`는 auto-config 등록이 보장되지 않으므로, “enabled 토글”을 유의미하게 만들려면 컨트롤러 등록 방식부터 정리해야 한다.

## 6) 추가 개발 항목(요약)
- `ControlFacade`(서비스) 정의 + 검증/인가/감사 공통 처리
- `/api/control/**` 컨트롤러 구현 + static token 인증 필터
- AI Tool 구현이 `ControlClient`(인터페이스)만 의존하도록 정리:
  - `local` 구현: `ControlFacade` 직접 호출
  - `remote` 구현: `ControlHttpClient`로 Control API 호출
- Web UI 리소스를 “공용 모듈(예: `ai-web-ui`)”로 분리해 두 앱이 동일 리소스를 제공하도록 정리
- `AiChatController`의 등록/구성 정리(두 앱 모두에서 `/api/ai/**`가 프로퍼티로 제어되도록)
