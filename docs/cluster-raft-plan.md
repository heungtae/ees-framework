# RaftServer & StateMachine Implementation Plan

## 목적
- `cluster` 모듈에 Apache Ratis 기반 `RaftServer`와 `StateMachine`을 추가해 락/할당 상태를 합의로 관리한다.
- 스냅샷/로그 관리 전략을 정의해 장애 복구와 리밸런스 시 빠른 부트스트랩을 보장한다.

## 전제/스코프
- 하나의 `RaftServer` 프로세스가 여러 `RaftGroup`을 관리한다. 그룹별 peers/StateMachine을 등록하는 방식을 사용한다(생성자 주입 대신 register API).
- 직렬화 포맷은 JSON으로 고정 시작(필요 시 버전 필드 포함).
- 스냅샷 저장은 설정에 따라 `file`/`db`/`kafka-ktable` 중 선택(`ees.cluster.raft.snapshot.store=file|db|kafka-ktable`).
- 명령 종류: `LockCommand`, `ReleaseLock`, `AssignPartition`(설비 리스트 포함), `RevokePartition`, `AssignKey`, `UnassignKey`.
- Ratis를 직접 사용하며 gRPC/Netty 트랜스포트 기본값을 따른다.
- 애플리케이션 수만큼 RaftGroup을 동적으로 추가할 수 있어야 한다. 신규 애플리케이션 합류 시 그룹 생성 + 설비 분산은 명령으로 트리거된다(자동 분산 금지).

## 설계 방향
- **Server 부트스트랩**
  - 단일 `RaftServer` 인스턴스 생성/시작/정지를 관리하는 `RaftServerFactory`.
  - 그룹별로 `registerGroup(RaftGroup, StateMachine)` 형태의 등록 API를 사용해 런타임에 여러 그룹을 추가한다(생성 시 주입 금지).
  - 설정 키: `ees.cluster.raft.data-dir`, `ees.cluster.raft.groups[app].peers[]`, `ees.cluster.raft.peer-count`(3 또는 5, 기본 3), `ees.cluster.raft.heartbeat-ms`, `ees.cluster.raft.rpc-timeout-ms`, `ees.cluster.raft.snapshot.threshold`, `ees.cluster.raft.snapshot.store`.
  - 노드 ID는 기존 `ClusterProperties.nodeId` 재사용, Ratis `RaftPeerId`로 변환.
  - **Peer 자동 구성**: 그룹 생성 시 피어는 rack-aware 분산 알고리즘으로 자동 선정한다.
    - 입력: (노드ID, rack, zone, 상태) 리스트, 목표 peer-count(3/5).
    - 알고리즘(간단한 균형 배치):
      1) 랙별로 노드 목록을 그룹화하고 health/가용 노드만 사용.
      2) 라운드로빈으로 랙을 순회하며 peer-count까지 선택해 분산을 최대로 한다.
      3) 랙 수 < peer-count인 경우, 남은 슬롯은 노드 수가 많은 랙부터 추가 선택(가중 라운드로빈).
      4) 동일 랙에서 중복 배치 시에도 동일 노드 중복은 금지.
    - 출력: 선택된 peer 리스트와 배치 근거(랙/zone 균형) 로그.
- **StateMachine**
  - `ClusterStateMachine` 구현:
    - 로그 적용 시 `RaftAssignmentService`/`DistributedLockService`에 반영.
    - 그룹별로 설비 할당 상태를 유지: `Assignment`에 equipmentIds가 포함되며, 그룹/파티션 단위로 업데이트.
    - 설비 라우팅 규칙: `AssignPartition` 명령에 포함된 설비가 어느 `groupId/partition`에서 처리될지 명시적으로 기록한다.
    - 상태 질의(ReadOnly)는 캐시 + 메타스토어에서 제공.
    - 커맨드 직렬화: JSON(`ByteString`) 사용, 버전/타입 필드 포함.
  - 상태 리더 일관성: 리더 전용 API(쓰기) + 팔로워/리더 모두 읽기 허용.
  - 다중 그룹: 그룹별 인스턴스를 생성하고 `register`로 서버에 붙인다.
- **Snapshot 전략**
  - 트리거: 적용된 엔트리 카운트 또는 로그 크기(`snapshot.threshold`) 기준, Ratis `takeSnapshot` 구현.
  - 스냅샷 저장소(`ees.cluster.raft.snapshot.store`):
    - `file`: 로컬 디렉터리(기본값).
    - `db`: `MetadataStore`(DB 백엔드) 키 `cluster:raft/snapshots/{group}`에 blob 저장.
    - `kafka-ktable`: `MetadataStore`(Kafka KTable 백엔드)로 blob/메타 저장.
  - 스냅샷 내용: 현재 락 맵, 파티션 할당, 키 할당 전체를 JSON 직렬화.
  - 스냅샷 로드: `initialize`/`reinitialize`에서 `MetadataStore`(백엔드에 따라 file/db/kafka-ktable)로부터 읽어 캐시/메타스토어를 재구성.
  - 로그 압축: 스냅샷 이후 이전 로그 구간 트리밍.
- **관측/운영**
  - 메트릭: 리더 전환, 적용 지연, 스냅샷 횟수/크기, 재시도/오류.
  - 헬스: 리더 여부, 피어 연결 상태, 마지막 적용 인덱스, 스냅샷 최신 시각.
  - 설비 분산/리밸런스:
    - 기본 피어 수는 3, 옵션으로 5 선택 가능.
    - 새 애플리케이션이 추가되면 대응되는 RaftGroup을 생성하지만 설비 이동은 명령(`RebalanceEquipmentCommand` 등)으로만 수행한다.
    - 리밸런스는 그룹/파티션 단위로 1대씩 순차 적용하며, 이동 중 데이터 유실을 막기 위해 Source 측 버퍼링 또는 중복 소비를 허용하는 세이프 모드가 필요하다.
    - 리밸런스 완료 후 스냅샷/로그 커밋으로 상태를 고정하고, 리더 전환 시에도 일관성이 유지되도록 한다.

## 구현 태스크
1) **부트스트랩 스켈레톤**
   - `RaftServerFactory`, `ClusterStateMachine` 기본 뼈대, 설정/프로퍼티 추가.
2) **명령/직렬화 계층**
   - 명령 DTO 정의, 직렬화/역직렬화 유틸, 로그 적용 핸들러 구현.
3) **스냅샷 처리**
   - `takeSnapshot`, `loadSnapshot` 구현; 스냅샷 파일 포맷/버전 명세.
4) **운영 통합**
   - 메트릭/헬스 지표 노출, 종료 훅/재시작 경로 정리.
   - 애플리케이션 처리 규칙: 각 노드는 자신이 속한 그룹의 리더 여부를 확인하고, 리더만 입력(Source) 처리를 수행하며 팔로워는 리더 전환 시에만 활성화.
5) **테스트**
   - 단위: StateMachine 명령 적용/스냅샷 라운드트립.
   - 통합: 3노드 임베디드 Ratis로 리더 전환, 로그 재플레이, 스냅샷 복구.

## 체크리스트
- [x] `RaftServerFactory`와 `ClusterStateMachine` 스켈레톤 작성, 설정 키 연결.
- [x] 명령 DTO/JSON 직렬화 유틸 구현 및 락/할당 상태 적용 핸들러 작성.
- [x] 스냅샷 take/load 흐름과 파일/db/kafka-ktable 스토어 플러그형 처리, 포맷 버전 명시.
- [x] 메트릭·헬스 노출과 서버 시작/종료 훅 정리.
- [x] 리더 전용 처리 규칙과 리밸런스 세이프 모드 가드 추가.
- [x] StateMachine 단위 테스트와 3노드 임베디드 Ratis 통합 테스트 구성.

## 오픈 질문
- 명령 직렬화 포맷을 JSON으로 시작할지 protobuf를 바로 도입할지?
- 스냅샷 파일을 로컬에만 둘지 메타스토어 키로 복제할지?
- 그룹 다중화(멀티 앱) 지원 시 프로세스당 다중 RaftServer를 둘지, 멀티 그룹 지원으로 확장할지?
