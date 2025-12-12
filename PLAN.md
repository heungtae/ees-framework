# 작업 계획: Per-Key Ordered Parallel Execution 구현 준비

- [x] 워크플로/파이프라인 실행 구조 파악(BlockingWorkflowEngine 등 주요 클래스 읽기)
- [x] 키/affinity 관련 기존 컴포넌트 조사(FxContext, AssignmentService 등 검색)
- [x] Per-key 실행기/KeyResolver/큐 설정 확장 설계안 구체화(현 구조와 맞물리는 API 스케치)
- [x] 구현 범위/우선순위 결정 후 세부 작업 분할(클래스/테스트 단위)
- [x] 1단계 구현 착수 및 진행 상황 기록

## 설계 메모(초안)
- BlockingWorkflowEngine.DefaultWorkflow를 per-key 큐/워커 기반으로 재구성하고, 키별 순서를 유지하면서 병렬 처리하도록 변경. 워커는 virtual thread executor(newThreadPerTaskExecutor)에서 동작.
- AffinityKeyResolver는 defaultKind 업데이트를 계속 허용하며, FxContext.affinity 우선 → 헤더/메타 fallback 흐름 유지. normalize 단계에서 kind/value 누락 또는 defaultKind 불일치 시 예외.
- 배치/큐 옵션을 per-key 설정으로 확장(큐 용량, 배치 크기/타임아웃, idle 정리 시간, backpressure 정책 등)하고 DSL/엔진에 전달. 초기에는 BLOCK 정책만 구현하고 enum 확장으로 인터페이스 고정.
- Execution orchestrator 흐름: source iterable → affinity 계산 → per-key mailbox.enqueue → 워커 없으면 생성 → 워커가 batchSize/timeout 기준으로 drain 후 handler/step/sink 체인 수행 → idle 시 정리.
- ClusterAffinityKindMonitor → AffinityKindChangeHandler 경로로 kind 변경 시 워크플로 재바인딩/워커 리셋 훅을 준비(기본 구현은 engine.updateAffinityKind + runtime.rebindAll).
- 테스트: 키 불일치/누락 가드, 동일 키 순서 보장, 상이 키 병렬 처리, affinity kind 변경 시 업데이트 확인.

## 구현 세부 작업(초안)
- [x] 엔진 옵션 확장: per-key 백프레셔 정책 enum 및 PerKeyExecutionOptions 정의(큐 용량, 배치 크기/타임아웃, idle 정리 시간 포함)
- [x] PerKeyMailbox/Worker 스켈레톤 작성(virtual thread 기반 실행, enqueue/dequeue, idle 정리)
- [x] BlockingWorkflowEngine.DefaultWorkflow를 per-key orchestrator로 리팩터링(워커 생성/드레인, handler/step/sink 적용 흐름 포함)
- [x] 기본 테스트 추가(동일 키 순서 보존, 상이 키 병렬 처리, affinity kind 가드 유지)

## 다음 단계 작업
- [x] BackpressurePolicy(DROP_OLDEST, ERROR) 구현 및 테스트 추가
- [x] Workflow DSL/설정에서 per-key 옵션 노출(queueCapacity, batchSize/Timeout, cleanupIdleAfter, backpressurePolicy 등)
- [x] Cluster affinity kind 변경 이벤트 시 워커 재바인딩/큐 정리 흐름 통합 테스트 추가
- [x] Spring Boot 설정 연계: ees.workflow.* 프로퍼티를 BlockingWorkflowEngine.BatchingOptions로 바인딩하는 자동 설정 추가 및 단위 테스트
- [x] 설정 사용법 문서화(예시 properties/yaml)
- [x] WorkflowProperties 검증 강화 및 실패 케이스 테스트 추가

# 작업 계획: MVC + Virtual Thread Migration 검토
- [x] `docs/mvc-virtual-thread-migration-plan.md` 내용 정독
- [x] 주요 전환 포인트/위험 요약
- [x] 실행 단계 제안(우선순위/담당 모듈)
- [x] 결과 정리 및 공유

# 작업 계획: Metadata Store Plan 검토
- [x] `docs/metadata-store-plan.md` 내용 정독
- [x] 핵심 요구사항/아키텍처 요약
- [x] 실행 단계 제안
- [x] 결과 정리 및 공유

## Metadata Store 구현 단계
- [x] MetadataStoreFactory/TTL 유틸/직렬화 기본기 추가
- [x] FileMetadataStore 구현 및 만료 스캐너/이벤트 발행
- [x] JDBC DbMetadataStore 구현 + 만료 정리 태스크
- [x] Kafka KTable backend 구현 + watch/scan 패턴 정리
  - [x] Kafka Streams 토폴로지/스토어 구성 및 직렬화 설정
  - [x] put/get/delete/batchPut/scan/watch TTL 처리 및 이벤트 발행 구현
  - [x] MetadataStoreFactory에서 Backend.KAFKA_KTABLE 생성 지원
  - [x] Kafka 백엔드 단위/계약 테스트 추가
- [x] 공통 TCK 테스트 백엔드별 재사용 및 Raft 스냅샷 키 표준 반영
  - [x] 계약 테스트에서 스냅샷 키 규약 케이스 추가
  - [x] Kafka 포함 각 백엔드에 계약 테스트 적용 및 정리
