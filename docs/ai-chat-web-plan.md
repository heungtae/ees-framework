# AI Chat Web UI Plan (Spring Boot static)

## 목표/스코프
- 기존 `/api/ai/chat`(sync)와 `/api/ai/chat/stream`(SSE) API를 사용하는 경량 Web UI 제공.
- 별도 프론트 빌드 도구 없이 Spring Boot 정적 리소스(`application/src/main/resources/static`)로 제공.
- 필수 UX: 메시지 입력, 세션/유저 ID 지정, 툴 목록 입력, 승인 헤더 설정, 스트리밍 응답 표시.

## 기능 요구사항
- 입력 폼: message textarea, sessionId/userId 필드(기본값 생성), toolsAllowed(콤마 구분), 승인 체크박스(`X-AI-Approve`), streaming on/off 토글.
- 출력: 채팅 transcript 영역 + 스트리밍 시 토큰/청크 실시간 표시; 완료 시 최종 응답 표시.
- 에러: 유효성 오류/HTTP 오류 메시지 표시; danger 툴 승인 누락 시 안내.
- 상태: 로딩 인디케이터, 마지막 요청 시간, SSE 연결 상태 표시.

## 아키텍처/구현
- 정적 파일: `application/src/main/resources/static/ai-chat.html` + `ai-chat.js` + 간단한 CSS.
- 데이터 흐름:
  - Sync: `fetch` POST `/api/ai/chat`.
  - Stream: `EventSource` `/api/ai/chat/stream`, `chunk` 이벤트는 transcript에 append, `complete`에서 연결 종료.
- 설정/기본값:
  - base URL: 현재 호스트 기준 상대 경로.
  - sessionId 기본 UUID 생성(페이지 로드 시), userId 기본 `web-user`.
  - 승인 체크 시 헤더 `X-AI-Approve: true`.
- 접근성/모바일: 반응형 단일 컬럼 레이아웃, 키보드 제출 지원(Ctrl+Enter).

## 화면 개략
- 상단 제어: session/user/stream toggle/approve checkbox/tools 입력.
- 본문: 메시지 textarea + send buttons (chat / stream).
- 출력 패널: chunk 로그, 최종 응답 강조.

## 향후 확장
- 메시지 히스토리 캐싱(localStorage).
- 다중 세션 탭, 툴 결과 카드 렌더링.
- 위험 명령 재확인 모달, 테마 전환.
