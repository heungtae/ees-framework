# AI Chat CLI Plan (REST/SSE backed)

## 목표/스코프
- 기존 `/api/ai/chat`(sync)와 `/api/ai/chat/stream`(SSE) 엔드포인트를 이용하는 CLI 클라이언트 제공.
- 메시지 입력, 세션/사용자 ID 유지, 툴 승인 헤더(`X-AI-Approve`) 전달, 스트리밍 응답 표시를 지원.
- 설치/빌드 의존성 최소화(go/java/node 중 하나 선택) — 우선 Node.js + fetch/EventSource 간단 구현 목표.

## 기능 요구사항
- `chat`: 단발 프롬프트 → 단일 응답 출력.
- `stream`: 스트리밍 모드로 토큰/청크 표시, 완료 시 최종 메시지 표시.
- 세션 관리: `--session` 옵션 또는 ENV(`AI_SESSION_ID`), 자동 생성 UUID fallback.
- 사용자 식별: `--user` 옵션 또는 ENV(`AI_USER_ID`), 기본 `cli-user`.
- 툴 승인: `--approve` 플래그 → `X-AI-Approve: true` 헤더.
- 모델/도구 힌트: `--tools listNodes,describeTopology` 전달(REST payload `toolsAllowed`).
- 출력 모드: plain(default) vs json(`--json`), 스트리밍 시 각 chunk를 한 줄씩 표시.
- 에러 처리: HTTP/validation 에러를 읽기 쉬운 메시지로 출력, 종료 코드 1.

## 아키텍처/구현
- 언어: Node.js 스크립트(`tools/ai-chat-cli.js` 등)로 단일 파일 CLI.
- 라이브러리: 기본 `node-fetch`/`undici` + `eventsource`(SSE 폴리필) 또는 native `fetch`가 지원되면 의존 최소화.
- 커맨드 파서: 경량(`commander` 또는 자체 파싱) — 의존 최소화를 위해 자체 파싱 고려.
- 설정: ENV + 옵션 병합
  - `AI_API_BASE` (기본 `http://localhost:8080`)
  - `AI_SESSION_ID`, `AI_USER_ID`
  - `AI_APPROVE=true` (플래그 대체 가능)
- 요청 구조:
  ```json
  {
    "sessionId": "...",
    "userId": "...",
    "messages": [{ "role": "user", "content": "..." }],
    "toolsAllowed": ["..."],
    "streaming": true|false
  }
  ```
- 스트리밍 처리: SSE `event: chunk|complete`, `data`를 파싱해 순차 출력; complete 이벤트에서 종료.

## 커맨드 예시
- `./ai-chat --text "hello" --session s1`
- `./ai-chat stream --text "hi" --tools listNodes --approve`
- `AI_API_BASE=https://app/api ./ai-chat --text "status" --json`

## 향후 확장
- 멀티턴 대화: 프롬프트 히스토리 파일/디렉터리 지원.
- Tool result pretty-print, 위험 명령 확인 프롬프트.
- TUI(ink/blessed) 기반 채팅 UI 확장.
