# 클러스터 모듈 설계

`cluster` 모듈은 멤버십, 리더 선출, 분산 락, 할당 서비스를 제공해 다른 모듈(workflow, source/handlers/pipeline/sink)에서 재사용한다. 대상: Java 17, Spring Boot 3.3, Reactor 3.6.

## 목표
- 멤버십/리더십/락/할당을 반응형 API로 제공.
- 상태 저장은 metadata-store를 통해 수행하고, 브로드캐스트는 필요 시 메시징 모듈로 위임.
- TTL/CAS 기반으로 분할 브레인을 피하고, 관측성을 노출.
- 운영 모드를 두 가지로 제공: Kafka 모드(컨슈머 할당 기반 active/standby)와 Raft 모드(Apache Ratis), 애플리케이션/워크로드별로 선택.

## 모드 전략
- Kafka 모드
  - Kafka 컨슈머 그룹 할당으로 active/standby를 결정하며, 파티션을 가진 노드가 해당 샤드의 active가 되고 나머지는 standby가 된다.
  - 헬스/가시성을 위해 `ClusterMembershipService`로 멤버십은 기록하지만, 토픽별 리더십은 `cluster:leader` CAS가 아니라 Kafka 할당에 위임.
  - `AssignmentService`가 컨슈머 `assigned/revoked` 콜백을 수신해 `TopologyEvent`를 발행해 파이프라인 소유권을 조정한다. 분산 락은 Kafka가 조정하지 않는 작업에 한해 사용.
  - 파티션→설비/워크플로 매핑을 metadata-store에 기록해, 리밸런스 시 새 오너가 직전 워크플로 상태·체크포인트를 인수할 수 있게 한다.
  - 장애 조치는 Kafka 리밸런스에 의해 처리되며, heartbeat는 정보용/모니터링 용도.
- Raft 모드(Apache Ratis)
  - 애플리케이션/워크로드마다 Raft 그룹을 구성하고, 각 그룹의 peers를 설정(예: 존/설비 단위)해 메타스토어나 설정에 저장한다.
  - 그룹 상태 머신이 리더십, 락, 할당을 관리하며 `LeaderElectionService`가 Ratis 클라이언트/서버를 감싼다.
  - 새 멤버가 합류할 때는 리더가 조인 요청을 Raft 로그에 커밋해 구성원을 확장하고, 기존 peer가 빠르게 스냅샷을 전송한다.
  - 멤버십/heartbeat는 관측성용으로 유지하되, 권한은 Raft 로그에 의해 결정되어 split-brain을 회피한다.
  - 설비/리소스를 그룹별로 묶어 해당 Raft 그룹에서만 할당을 수행해 지역성을 높이고 장애 범위를 축소한다.

## 핵심 컴포넌트
- `ClusterNode` / `ClusterNodeInfo`: 노드 ID, host/port, 역할(source/handler/pipeline/sink/workflow), zone/region, 메타데이터.
- `ClusterStateRepository`: metadata-store 기반 영속 추상화(CAS, TTL/lease 지원).
- `ClusterMembershipService`: join/leave, heartbeat 기록, 클러스터 뷰 캐시, `Flux<MembershipEvent>`.
- `HeartbeatMonitor`: 주기적 heartbeat 스케줄링; 타임아웃을 `SUSPECT` → `DOWN`으로 전이 감지.
- `LeaderElectionService`:
  - Kafka 모드: CAS 없이 컨슈머 할당 이벤트로 파티션/토픽별 active/standby를 추론; Kafka 외 작업만 단일 리더 CAS를 선택적으로 사용.
  - Raft 모드: Ratis 클라이언트/서버를 감싸 `Mono<LeaderInfo>`와 변경 스트림을 제공; 애플리케이션별 그룹 생성을 관리.
- `DistributedLockService`: 짧은 lease 기반 락; Kafka 모드에서는 워크플로 스케줄링/소유권 보호용, Raft 모드에서는 Ratis 상태 머신을 통해 보증.
- `AssignmentService`:
  - Kafka 모드: 컨슈머 `assigned/revoked` 이벤트를 받아 파티션→노드 매핑을 갱신하고 토폴로지 이벤트를 발행.
  - Raft 모드: 리더가 리밸런스를 수행하고 Raft 로그에 커밋; 팔로워는 상태 머신을 적용해 동기화.
- `ClusterEventPublisher`: 내부 이벤트 버스는 Reactor Flux, 필요 시 메시징 모듈과 브리지해 브로드캐스트.

## 데이터 모델(metadata-store 키)
- `cluster:nodes/{nodeId}`: {status, lastHeartbeat, roles, zone, version, metadata}, TTL = heartbeatTimeout * 2.
- Kafka 모드: Kafka가 조정하는 워크로드는 `cluster:leader`를 사용하지 않음(비-Kafka 작업만 선택적). 할당은 컨슈머 뷰에서 파생.
- Raft 모드: `cluster:raft/groups/{app}` -> {groupId, peers, equipmentGroup}, `cluster:raft/metadata/{app}` 부트스트랩 정보. 리더/락/할당 상태는 Raft 로그에 지속.
- 공통(모드 무관): `cluster:locks/{name}`: {ownerNodeId, leaseUntil, meta}(또는 Raft 백엔드), `cluster:assignments/{group}`: {partition -> {ownerNodeId, equipmentIds, workflowHandoff, lastOffset, version}}.
- Raft 조인 요청: `cluster:raft/join-requests/{app}/{nodeId}`: {requestedAt, zone, roles, endpoint, metadata}, TTL = joinTimeout. 리더가 승인/거절 후 삭제.

## 라이프사이클
1) 시작: `join(nodeInfo)`로 노드 레코드를 생성/갱신하고 heartbeat 루프를 시작.
2) 리더십:
   - Kafka 모드: 컨슈머 할당이 active/standby를 결정; 비-Kafka 작업만 단일 리더(CAS) 선택.
   - Raft 모드: 애플리케이션별 Ratis 그룹이 리더를 선출; 리더 정보는 `LeaderElectionService`로 노출.
3) Heartbeat: 주기적 기록; 타임아웃 시 `SUSPECT` → `DOWN` 전이. Raft 피어 헬스 체크에도 활용.
4) 할당:
   - Kafka 모드: Kafka 리밸런스 콜백 기반으로 토폴로지 변경을 발행.
   - Raft 모드: 리더가 `rebalance()` 실행 후 Raft 로그에 커밋, 팔로워는 적용.
5) 종료: `leave()`로 상태를 `LEFT`로 전이하고 lease를 정리. Kafka 컨슈머는 종료해 리밸런스를 유도, Raft 노드는 step down.

## 장애 처리
- Kafka 모드: 장애 조치는 Kafka 리밸런스에 위임; 클러스터 heartbeat는 알림/모니터링용. 분산 락은 TTL로 고아 소유자 제거.
- Raft 모드: Ratis 쿼럼이 lease/term 규칙을 보증; 리더 lease 갱신 실패 시 즉시 step down. 멤버십 TTL로 죽은 노드 정리, metadata-store 오류 시 백오프+jitter.

## 설정(spring-boot-starter)
- 공통: `ees.cluster.heartbeat.interval`, `.timeout`, `.lease.duration`, `.roles`, `.zone`, `.assignments.rebalance-interval`.
- Kafka 모드: `ees.cluster.mode=kafka`, 컨슈머 그룹/토픽 바인딩, 파티션→역할 매핑.
- Raft 모드: `ees.cluster.mode=raft`, 애플리케이션별 Ratis peers(`ees.cluster.raft.groups[app].peers`), 설비/리소스 그룹 설정, RPC/복제 타임아웃.
- 빈: 리포지토리, 멤버십, 리더십(Kafka vs Ratis), 락, 할당용 자동 구성. 애플리케이션 라이프사이클 훅(join/leave/컨슈머 이벤트 포함).

## 관측성
- Micrometer: heartbeat 지연, 리더 변경, 락 경합, 노드당 할당 수 등.
- HealthIndicator: 클러스터 준비 여부와 리더 상태를 보고.
- Kafka 파티션 핸드오프: 핸드오프 기록(latency/크기), 재시도 횟수, assign→ready까지 걸린 시간.
- Raft 핸드오프: 리밸런스 커밋 지연, 스냅샷 전송 시간, 새 노드가 ready 되기까지 걸린 시간.

## Raft 모드 멤버 조인 API
- 동기화 목표: Kafka 컨슈머 그룹처럼 신규 노드가 `join()` 호출만으로 참여/할당을 받을 수 있도록 하되, 합류는 Raft 리더가 합의해 안전하게 구성원을 늘린다.
- API 형태(초안):
  - `Mono<JoinTicket> requestJoin(RaftJoinRequest request)`: 후보 노드가 리더에게 zone/roles/endpoint를 제출. 리더는 `join-requests` 키에 기록 후 Raft 로그에 `AddPeer` 의사를 제안.
  - `Mono<JoinAcceptance> awaitAcceptance(JoinTicket)`: 리더가 과반수 승인 시 `JoinAcceptance` 반환(할당된 peerId, 스냅샷 주소, effectiveIndex 포함). 거절 시 이유 포함.
  - `Mono<Void> completeSync(peerId)`: 신규 노드가 스냅샷/로그를 받아 동기화 완료 후 리더에게 보고. 리더는 완료 이벤트를 로그에 커밋해 최종 반영.
- 흐름:
  1) 신규 노드가 `requestJoin` 호출 → 리더가 라우팅(팔로워는 리디렉션 응답).
  2) 리더가 Ratis `addPeer`(또는 재구성) 제안 → 커밋 시 `JoinAcceptance` 발행, 스냅샷 정보를 제공.
  3) 신규 노드가 스냅샷/로그 동기화 → `completeSync` 호출 → 리더가 상태 머신에 적용해 활성화.
  4) 활성화 후 기존 `AssignmentService`가 리더 주도로 리밸런스(할당 로그 커밋)하여 새 노드에 작업을 배정.
- 제약/시간 제한:
  - `joinTimeout` 내에 승인/동기화 실패 시 요청은 만료되고 키는 정리된다. 실패 시 노드는 재시도 또는 매뉴얼 승인 흐름 사용.
  - 구성 변경 동안 리더 변경이 발생하면 새 리더가 진행 중 요청을 재검증 후 이어받거나 거절한다.
- 관측성: 승인 지연, 실패 사유, 스냅샷 크기/소요 시간, 조인 후 첫 리밸런스 시간(metrics + 이벤트 스트림) 노출.

## Kafka 모드: 파티션/설비 매핑 & 워크플로 핸드오프
- 목표: 동일 컨슈머 그룹 내 리밸런스 시에도 파티션이 담당하던 설비와 진행 중 워크플로 상태를 새 오너가 이어받아 중단 없이 처리.
- 데이터/키:
  - `cluster:assignments/{group}/{partition}`: {ownerNodeId, equipmentIds[], workflowHandoff(업스트림 체크포인트, 워크플로 단계 상태, lastProcessedAt), lastOffset, version, updatedAt}.
  - `TopologyEvent`에 `equipmentIds`와 `workflowHandoff`를 포함해 소비 측 파이프라인이 초기화에 활용.
- 흐름:
  1) `assigned(partitions)` 수신 시: 각 파티션에 대한 `workflowHandoff` 조회 → 파이프라인 초기화 시 전달 → 오프셋 seek 후 처리 시작.
  2) `revoked(partitions)` 수신 시: 파티션별 최신 워크플로 체크포인트/메타데이터를 수집하고 `workflowHandoff`로 기록 후 커밋 → Kafka가 재할당.
  3) 장애로 종료되면 마지막 커밋 오프셋과 직전 핸드오프 데이터로 복구; 핸드오프 TTL은 Kafka retention보다 길게 설정.
- 고려사항:
  - 핸드오프 기록 실패 시 재시도하며, 실패 누적 시 빠른 알림 후 워크플로 측이 별도 복구 경로를 사용하게 한다.
  - `equipmentIds`가 많은 경우 압축 또는 외부 스냅샷 경로를 저장(메타스토어에는 포인터).
  - 동일 파티션 내 순서 보장을 위해 핸드오프는 revoke 후 seek 지점 이전 오프셋 기준으로 기록.

## Raft 모드: 워크플로/설비 핸드오프
- 목표: 리더 주도 리밸런스 시 설비/워크플로 상태를 함께 로그에 커밋해, 새 오너가 즉시 이어받게 한다.
- 방식:
  - 리더가 `rebalance()` 시 `cluster:assignments` 상태 머신 항목에 {partition, owner, equipmentIds, workflowHandoff, lastAppliedIndex}를 커밋.
  - 팔로워는 로그 적용 시 동일한 핸드오프 데이터를 로컬에 반영하고, 노드가 활성화될 때 `TopologyEvent`로 노출.
  - 노드 다운 후 복귀하거나 새 피어가 합류할 때 스냅샷/로그 재생으로 핸드오프 정보를 함께 복원.
- 고려사항:
  - 핸드오프 크기가 크면 별도 스냅샷 채널을 제공하고 로그에는 참조만 남긴다.
  - 리밸런스 중 리더 변경 시 lastAppliedIndex를 비교해 중복 적용을 피하고, 새 리더가 미완료 핸드오프를 재평가해 재커밋.

## 테스트 전략
- 단위: 인메모리 `ClusterStateRepository`로 멤버십/heartbeat/리더/락 흐름 검증(Reactor StepVerifier).
- 통합: 가짜 metadata-store로 TTL/CAS, 이중 리더 방지 검증. Ratis는 임베디드/메모리 트랜스포트로 리더 전환/로그 적용 테스트.
- 스타터: 프로퍼티 바인딩, 조건부 자동 구성 테스트; 스프링 컨텍스트 최소화.

## 다음 단계
1) 공용 인터페이스와 인메모리 구현을 추가해 API를 고정.
2) metadata-store 기반 구현(CAS/TTL)과 Kafka 모드 연동(컨슈머 콜백) 추가.
3) Raft 모드: Ratis 그룹/피어 설정, 상태 머신(락/할당) 정의, 리더 이벤트 노출.
4) Spring Boot 자동 구성, 헬스/프로퍼티 바인딩 정리.
5) 워크플로/파티션 할당 PoC로 API 적합성 확인.
