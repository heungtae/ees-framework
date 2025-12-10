# Affinity 정합성 남은 작업 계획

- [x] 코드베이스에서 affinity 키 처리 흐름과 AssignmentService 연계 지점 조사
- [x] AssignmentService 어댑터/추출기가 `assignKey(..., kind, key, ...)` 호출을 기본 사용하도록 수정
- [x] Cluster 토폴로지 이벤트를 구독해 affinity kind 변경 시 워크플로 워커/큐 재바인딩 구현(ClusterAffinityKindMonitor → WorkflowEngine.updateAffinityKind 연계)
- [x] 테스트 추가: kind 불일치 거부/로그, affinity 변경 후 워커 재바인딩, 헤더 fallback 시 kind 지정 검증
