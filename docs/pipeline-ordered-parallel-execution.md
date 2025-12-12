# Per-Key Ordered Parallel Execution Plan

## 목표
- Source가 내보내는 레코드를 **키별(예: 설비별) 순서를 보장**하면서 병렬 처리해 처리량을 높인다.
- 기본 키는 설비 ID 등 도메인 키로 하지만, 애플리케이션이 자유롭게 키 추출 전략을 바꿀 수 있게 한다.
- 가상 스레드 기반으로 키별 워커를 분리해 스레드 관리 비용을 최소화한다.
- **cluster affinity**(partition/raftgroup 키)와 파이프라인 키를 일치시켜, cluster 재할당/키 변경 시 파이프라인 워커/큐가 동일하게 전환되도록 한다.

## 요구사항
- 동일 키의 레코드는 도착 순서대로 Sink까지 도달해야 한다.
- 서로 다른 키는 병렬 처리 가능하다.
- 키 추출 전략을 주입/구성 가능해야 한다(예: 헤더 `equipmentId`, 메시지 payload 필드, 커스텀 함수).
- backpressure: 각 키별 큐에 용량 제한을 두고, 초과 시 대기/드롭/에러 정책을 선택 가능하다.
- 장애 시 동일 키의 처리는 재시도/중단 정책을 선택 가능하다.
- 파이프라인 키는 **cluster assignments가 관리하는 파티션 키**(기본 `equipmentId`, 필요 시 `lotId` 등 affinity kind)이며, 병렬 처리는 이 cluster affinity 단위로 수행되어야 한다. cluster에서 affinity kind/값 변경 또는 재할당이 발생하면 파이프라인 키 매핑과 워커도 동일하게 갱신된다.

## 제안 아키텍처
- **KeyResolver**: `FxContext<?> -> AffinityKey{kind,value}`를 반환. 기본 구현은 cluster가 노출한 affinity kind(기본 `equipmentId`, 설정/추출기 교체로 `lotId` 등)를 우선 사용하고, 없으면 헤더/메타에서 찾은 값을 kind와 함께 반환한다. 애플리케이션은 빈/람다로 대체 가능.
- **PerKeyMailbox**: 키(kind+value)별 bounded 큐(`ArrayBlockingQueue` 등). 큐가 없으면 생성, 비어 있으면 일정 시간 후 정리.
- **PerKeyWorker**:
  - 단일 워커(virtual thread) 당 한 키(kind+value)를 전담해 순서 보장.
  - 큐에서 batchSize/time 기반으로 드레인 후 파이프라인/싱크에 전달.
  - 워커 수는 활성 키 수에 따라 증감; 정리 시 graceful shutdown.
- **Execution Orchestrator**:
  - Source로부터 들어온 레코드에 대해 KeyResolver로 affinity key 계산(kind+value).
  - 해당 affinity 키의 큐에 enqueue; 큐가 가득 차면 backpressure 정책 적용.
  - 워커가 없으면 생성 후 가상 스레드 실행.
  - Stop 시 모든 워커에 draining 신호를 보내고 큐를 비운다.

### Cluster 연계(키 정합성)
- cluster assignment는 `{kind -> values}` affinity 맵을 보관하며, 기본 kind는 `ees.cluster.assignment-affinity-kind`(기본 `equipmentId`). `KeyResolver`는 이 affinity kind/값을 **파이프라인 키로 그대로 사용**해야 한다.
- cluster 측에서 affinity kind 변경(예: `equipmentId`→`lotId`) 혹은 값 재할당이 발생하면 레지스트리 이벤트/캐시 갱신을 통해 파이프라인 키 매핑을 즉시 반영한다. 변경된 affinity에 대응해 기존 큐/워커를 폐기하고 새 affinity 키로 워커를 재생성해 병렬/순서 보장을 유지한다.
- 파이프라인 단계/Sink는 cluster가 보장한 affinity 키(kind+value)를 신뢰하며, 키 변환/별칭은 레지스트리/설정(추출기 교체, affinity kind 설정)으로만 허용한다.
- 키 할당/라우팅 시 `AssignmentService.assignKey(..., kind, key, ...)` 또는 추출기 기반 헬퍼를 사용해 kind가 일치하도록 보장한다.

## 동시성/순서 보장
- 동일 키: 단일 워커 + 순차 드레인으로 순서 보존.
- 상이한 키: 서로 다른 워커에서 병렬 처리.
- 파이프라인 단계는 순서가 깨지지 않도록 키별 워커 내부에서 실행하거나, 단계별로 동일 워커가 호출되도록 한다(스테이지 파이프라이닝 대신 키 전담 워커 모델 유지).

## 구성 옵션(제안)
- `queueCapacity`: 키별 큐 용량.
- `batchSize`, `batchTimeout`: 배치 드레인 기준.
- `cleanupIdleAfter`: 워커/큐가 비었을 때 종료까지 대기 시간.
- `backpressurePolicy`: `BLOCK`(기본) | `DROP_OLDEST` | `ERROR`.
- `keyResolver`: 빈/람다 주입; 기본은 cluster affinity kind(`ees.cluster.assignment-affinity-kind`, 기본 `equipmentId`) → 헤더/메타에서 값 추출 → fallback 키.
- `clusterPartitionKey/affinityKind`: cluster 모듈이 노출하는 affinity kind를 파이프라인이 우선 사용해야 함.
- `continuous`: true 시 Source 스트림을 계속 읽고 stop 신호까지 유지.

### Spring Boot 설정 예시
```yaml
ees:
  cluster:
    assignment-affinity-kind: equipmentId
  workflow:
    queue-capacity: 512
    batch-size: 64
    batch-timeout: 200ms
    cleanup-idle-after: 30s
    backpressure-policy: BLOCK # 또는 DROP_OLDEST / ERROR
    continuous: false
```

`application.yml`에 위 옵션을 지정하면 `BlockingWorkflowEngine`이 동일한 값으로 초기화된다. 잘못된 값(음수/0, null 타임아웃 등) 입력 시 시작 단계에서 예외로 가드된다.

## 모니터링/장애 대응
- 메트릭: 키별 큐 길이, 드롭/에러 카운트, 처리 지연, 워커 수.
- 로그/이벤트: backpressure 발생, 워커 생성/종료, 예외 발생 시 키 포함 로그.
- 실패 정책: 단일 레코드 실패 시 재시도/스킵 선택; 스킵 시 순서 유지 후 진행.

## 적용 경로
1) `BlockingWorkflowEngine`에 KeyResolver + per-key mailbox/worker 실행기 추가(현 배치/큐 옵션 재사용).
2) Workflow DSL에 키/큐/배치 옵션 노출(`perKey`, `keyResolver`, `queueCapacity`, `batchSize`, `batchTimeout`, `backpressurePolicy`).
3) cluster assignments 레지스트리 조회 기반의 기본 KeyResolver를 도입하고, cluster 파티션 키(equipmentId/lotId 등) 변경 이벤트가 워크플로 런타임에 전달되도록 연계한다(키 변경 시 큐/워커 초기화 포함). 병렬 처리 단위가 cluster key임을 검증하는 테스트 추가.
4) 기본 예제/테스트를 파티션 키로 업데이트하고, 커스텀 키 추출 테스트 추가.
5) 메트릭/로그 훅 추가 후, 전 모듈 빌드/테스트.

## Affinity 단일화 계획(context/cluster/pipeline)
- 목표: cluster가 관리하는 affinity 키(kind+value)를 **context(FxContext)·pipeline·cluster** 전역에서 동일 값으로 유지.
- 원칙:
  - 단일 소스: `ees.cluster.assignment-affinity-kind`(기본 equipmentId) + 추출된 value가 전역 키.
  - FxContext에 `affinity.kind`, `affinity.value` 필드를 저장하고 KeyResolver는 이 필드만 사용(헤더/메타 fallback은 마지막 수단).
  - Cluster assign/토폴로지 이벤트와 pipeline KeyResolver/워커의 kind가 불일치하면 처리 중단/로그로 가드.
  - Affinity kind 변경 이벤트가 오면 기존 큐/워커를 폐기 후 새 kind로 재바인딩.
- 구현 체크리스트:
  - [x] FxContext(또는 파이프라인 컨텍스트)에 `affinity.kind/value` 추가, 기본값은 cluster affinity kind(or none).
  - [x] 기본 KeyResolver를 affinity 전용으로 고정(컨텍스트 필드 우선, 헤더/메타는 fallback).
  - [x] AssignmentService 사용 시 `assignKey(..., kind, key, ...)` 호출이 기본이 되도록 어댑터/추출기 연계.
  - [x] Cluster 토폴로지 이벤트를 구독해 affinity kind/값 변경 시 파이프라인 워커/큐를 재생성. (ClusterAffinityKindMonitor 제공: assignment 이벤트에서 kind 감지 후 WorkflowEngine.updateAffinityKind 호출하도록 연결)
  - [x] 테스트: kind 불일치 시 거부/로그, affinity 변경 후 워커 재바인딩, 헤더 fallback 시 kind 지정 확인.

## 오픈 이슈
- 파이프라인 스텝이 키 무관 공유 리소스를 쓸 때의 동시성 제어(락/스레드 안전성) 가이드 필요.
- Sink 단에서 idempotency/재시도 전략 정의 여부.
