# 작업 계획: Per-Key Ordered Parallel Execution 구현 준비

- [x] 워크플로/파이프라인 실행 구조 파악(WorkflowEngine 등 주요 클래스 읽기)
- [x] 키/affinity 관련 기존 컴포넌트 조사(FxContext, AssignmentService 등 검색)
- [x] Per-key 실행기/KeyResolver/큐 설정 확장 설계안 구체화(현 구조와 맞물리는 API 스케치)
- [x] 구현 범위/우선순위 결정 후 세부 작업 분할(클래스/테스트 단위)
- [x] 1단계 구현 착수 및 진행 상황 기록

## 설계 메모(초안)
- WorkflowEngine.DefaultWorkflow를 per-key 큐/워커 기반으로 재구성하고, 키별 순서를 유지하면서 병렬 처리하도록 변경. 워커는 virtual thread executor(newThreadPerTaskExecutor)에서 동작.
- AffinityKeyResolver는 defaultKind 업데이트를 계속 허용하며, FxContext.affinity 우선 → 헤더/메타 fallback 흐름 유지. normalize 단계에서 kind/value 누락 또는 defaultKind 불일치 시 예외.
- 배치/큐 옵션을 per-key 설정으로 확장(큐 용량, 배치 크기/타임아웃, idle 정리 시간, backpressure 정책 등)하고 DSL/엔진에 전달. 초기에는 BLOCK 정책만 구현하고 enum 확장으로 인터페이스 고정.
- Execution orchestrator 흐름: source iterable → affinity 계산 → per-key mailbox.enqueue → 워커 없으면 생성 → 워커가 batchSize/timeout 기준으로 drain 후 handler/step/sink 체인 수행 → idle 시 정리.
- ClusterAffinityKindMonitor → AffinityKindChangeHandler 경로로 kind 변경 시 워크플로 재바인딩/워커 리셋 훅을 준비(기본 구현은 engine.updateAffinityKind + runtime.rebindAll).
- 테스트: 키 불일치/누락 가드, 동일 키 순서 보장, 상이 키 병렬 처리, affinity kind 변경 시 업데이트 확인.

## 구현 세부 작업(초안)
- [x] 엔진 옵션 확장: per-key 백프레셔 정책 enum 및 PerKeyExecutionOptions 정의(큐 용량, 배치 크기/타임아웃, idle 정리 시간 포함)
- [x] PerKeyMailbox/Worker 스켈레톤 작성(virtual thread 기반 실행, enqueue/dequeue, idle 정리)
- [x] WorkflowEngine.DefaultWorkflow를 per-key orchestrator로 리팩터링(워커 생성/드레인, handler/step/sink 적용 흐름 포함)
- [x] 기본 테스트 추가(동일 키 순서 보존, 상이 키 병렬 처리, affinity kind 가드 유지)

## 다음 단계 작업
- [x] BackpressurePolicy(DROP_OLDEST, ERROR) 구현 및 테스트 추가
- [x] Workflow DSL/설정에서 per-key 옵션 노출(queueCapacity, batchSize/Timeout, cleanupIdleAfter, backpressurePolicy 등)
- [x] Cluster affinity kind 변경 이벤트 시 워커 재바인딩/큐 정리 흐름 통합 테스트 추가
- [x] Spring Boot 설정 연계: ees.workflow.* 프로퍼티를 WorkflowEngine.BatchingOptions로 바인딩하는 자동 설정 추가 및 단위 테스트
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

# 작업 계획: 워크플로 per-key 실행/설정 마무리
- [x] 현재 워크플로/스프링 설정 변경분(diff) 리뷰 및 요구사항 재정렬
- [x] 누락된 테스트/문서 보완 여부 결정(필요 시 추가)
- [x] 모듈별 테스트 실행(워크플로, spring-boot-starter)
- [x] 커밋 및 요약 공유

# 작업 계획: 워크플로 모듈 Javadoc 정비
- [x] 워크플로 모듈 클래스/퍼블릭 메서드 Javadoc 대상 파악
- [x] 각 클래스와 주요 메서드에 역할/파라미터/반환/예외를 설명하는 Javadoc 추가
- [x] 변경 사항 자체 점검 및 필요 시 포맷/빌드 여부 결정

# 작업 계획: 워크플로 모듈 컴파일 오류 수정 및 커밋
- [x] 컴파일 오류 재현 및 원인 파악(mvn -pl workflow -DskipTests package)
- [x] 오류 원인 수정 및 로컬 재검증
- [x] 워크플로 모듈 변경사항 커밋

# 작업 계획: ai-core 모듈 오류 수정
- [x] ai-core 빌드/테스트에서 발생하는 오류 재현(mvn -pl ai-core -DskipTests package 등)
- [x] 오류 원인 분석 및 코드/설정 수정
- [x] 수정 후 로컬 재빌드/테스트로 검증
- [x] 필요 시 문서/추가 테스트 보강 여부 결정

# 작업 계획: ees-example 모듈 오류 수정
- [x] ees-example 빌드/테스트에서 발생하는 오류 재현(mvn -pl example -DskipTests package 등)
- [x] 오류 원인 분석 및 코드/설정 수정
- [x] 수정 후 로컬 재빌드/테스트로 검증
- [x] 필요 시 문서/추가 테스트 보강 여부 결정

# 작업 계획: application 모듈 오류 수정
- [x] application 빌드/테스트 오류 재현(mvn -pl application -DskipTests package 또는 clean test)
- [x] 오류 원인 분석 및 코드/설정 수정
- [x] 수정 후 로컬 재빌드/테스트로 검증
- [x] 필요 시 문서/추가 테스트 보강 여부 결정

# 작업 계획: example 모듈 AI 워크플로 추가
- [x] 요구사항 정리: 예시 AI 워크플로 시나리오 및 시스템 프롬프트 정의
- [x] 컴포넌트 설계: Source/Handler/PipelineStep/Sink 구성 및 스텁 AiAgentService 결정
- [x] 구현: 예시 AI 워크플로 컴포넌트/워크플로 정의 추가
- [x] 테스트: 예시 워크플로 E2E 테스트 추가 및 실행

# 작업 계획: Greeting 워크플로 AI 분류/트리아지 마무리
- [x] 현재 미커밋 변경사항 확인 및 정리(StubAiAgentService 변경 포함)
- [x] 트리아지 Sink 및 AI 분류 파이프라인 구성을 코드/테스트에 반영
- [x] example 모듈 테스트 실행으로 동작 검증
- [x] 필요한 추가 설명/문서화 여부 점검 및 최종 커밋 준비

# 작업 계획: GreetingSource 프롬프트 생성 책임 이동
- [x] GreetingSource에서 AI 프롬프트 제거하고 메시지 전달만 수행하도록 수정
- [x] 프롬프트를 생성/메타에 삽입하는 핸들러 로직 추가 및 테스트/구성 반영
- [x] example 모듈 테스트로 변경 검증

# 작업 계획: GreetingSourceHandler AI 로직 분리
- [x] GreetingSourceHandler에서 AI 프롬프트 로직 제거
- [x] AI 전용 SourceHandler 추가 및 워크플로/테스트 구성에 반영
- [x] example 모듈 테스트로 검증

# 작업 계획: AI 분류 설정/알림 후속 처리 추가
- [x] example 모듈에 AI 분류 프롬프트/지원 소스 설정을 프로퍼티로 추가
- [x] ALERT 분류 시 별도 알림 Sink로 전달하는 SinkHandler/Sink 구현
- [x] 워크플로/테스트에 새로운 SinkHandler/Sink를 반영하고 검증

# 작업 계획: core 모듈 Javadoc 정비
- [x] core 모듈 대상 클래스 점검
- [x] 컨텍스트/어노테이션/core API Javadoc 보강
- [x] `mvn -pl core -am test`로 검증
- [x] 변경사항 커밋

# 작업 계획: core 모듈 미사용 파일 정리
- [x] core 모듈 파일 목록 점검
- [x] 레포 전체 사용처(참조) 조사
- [x] 삭제 후보 검증(영향 확인)
- [x] 결론 기록: 현재 삭제 가능한 미사용 파일 없음

# 작업 계획: 전체 모듈 Javadoc 정비(Private 메서드는 주석)
- [x] 전 모듈 Javadoc 대상 스캔(퍼블릭 클래스/메서드 중심)
- [x] core/handlers/source/pipeline/sink/workflow Javadoc 보강
- [x] spring-boot-starter/ai-core/metadata-store/cluster/messaging Javadoc 보강
- [x] example/application(필요 최소) Javadoc 보강
- [x] private 메서드: 필요한 경우 주석으로 보강
- [x] `mvn clean test`로 전체 검증
- [x] 변경사항 커밋

# 작업 계획: Javadoc 가이드 문서 추가
- [x] `docs/javadoc.md` 작성
- [x] PLAN 체크리스트 업데이트
- [x] 변경사항 커밋

# 작업 계획: Kafka Source 추가

- [x] 기존 Source SPI/패턴 파악
- [x] Kafka Source 설계·API 확정
- [x] 구현 및 Spring 등록(autoconfigure)
- [x] 테스트·예제 설정 추가
- [x] 모듈 빌드로 검증

# 작업 계획: Kafka Sink 추가

- [x] 기존 Sink SPI/패턴 파악
- [x] Kafka Sink 설계·API 확정
- [x] 구현 및 Spring 등록(autoconfigure)
- [x] 테스트 추가
- [x] 모듈 빌드로 검증

# 작업 계획: Kafka Source/Sink builtin 이동 검토

- [x] builtin 패키지 의미/관례 정리
- [x] builtin 이동 시 영향(설정/의존성/호환성) 평가
- [x] 대안 구조(현행 유지 vs builtin 하위 패키지 vs 별도 모듈) 비교
- [x] 권고안/다음 액션 정리

# 작업 계획: workflow 모듈 init 로깅 보강

- [x] workflow 모듈 초기화 포인트 선정
- [x] INFO 로그 추가(런타임/엔진/등록/리바인드)
- [x] DEBUG 로그 추가(상세 설정/노드 요약/워커 생성)
- [x] 모듈 테스트로 검증
- [x] 체크리스트 완료 처리
