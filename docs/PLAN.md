## 단일 서비스: EES App(Web UI + AI Chat + Control API) 설계/구현 계획

### 목표
- Web UI를 2가지 방식으로 제공한다.
  - Standalone: `ai-web-application` 실행 후 접근
  - Embedded(기본): `ees-framework-application`에서 Web UI 직접 제공
- 제어는 “MCP 서버 호출”이 아니라 `ControlFacade/ControlClient`를 통해 수행한다(local/remote 전환).

### 체크리스트
- [x] 기존 MCP/분리 설계는 보관 처리(참고용)
- [x] 직접 제어 설계(Controller + Facade) 확정
- [x] 제어 API(`/api/control/**`) 범위/계약 확정
- [x] 보안(Static token) 정책 확정(MVP: 토큰 필수)
- [x] Web UI 듀얼 제공(Standalone+Embedded) 설계 확정(기본 Embedded)
- [ ] 구현 작업 항목(파일/모듈/테스트) 쪼개기 및 순서 확정

### 참고 문서
- `docs/direct-control-web-ui-design.md` (최신)
- `docs/multi-container-mcp-ai-chat-design.md` (보관)

---

## 실행 계획(초안, MVP → 확장)

### Phase 0: 결정(필수)
- [x] 워크플로 제어 모델: 런타임 제어형(옵션 A) 유지
- [x] 인증 방식: Static token(1차)
- [x] 제어 API Base path 확정: `/api/control/**`
- [x] `ControlFacade`가 제공할 메서드/검증 규칙 확정(MVP)
- [x] Standalone/Embedded 토글 프로퍼티 확정(`ees.ai.web.enabled`, `ees.control.web.enabled`, `ees.control.mode`)

### Phase 1: 직접 제어 API + AI Chat 노출(MVP)
- [x] `ControlFacade`(서비스) 도입: 클러스터/워크플로/락/할당 제어를 한 곳에서 제공
- [x] `/api/control/**` 컨트롤러 구현(조회/제어)
- [x] Static token 인증 필터 추가(서버 측)
- [x] `/api/ai/**` 자동 등록(`ees.ai.web.enabled`) 정리
- [x] `ControlClient` 인터페이스 도입 + `local/remote` 구현
- [ ] Web UI 리소스를 공용 모듈로 분리하고 두 앱이 동일 UI 제공(현재는 Standalone에 복사본 사용)

### Phase 2: 품질/운영(권장)
- [ ] SSE 스트리밍(토폴로지/워크플로 상태) 설계 및 구현
- [ ] 파라미터 검증 강화(파괴적 명령 차단), 레이트 리밋
- [ ] 메트릭/헬스 지표 추가
