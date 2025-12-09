# Metadata Store Backend Upgrade Plan

## 목표
- `MetadataStore`를 플러그형 백엔드(file/db/kafka-ktable)로 확장해 내구성 있는 상태 보관을 지원한다.
- Raft 스냅샷/락/할당 메타데이터 등 클러스터 주요 상태를 영속화할 수 있게 한다.

## 대상 기능
- 현재 `InMemoryMetadataStore` 외에 다음 구현을 추가한다:
  - `FileMetadataStore`: 단일 노드용 파일 기반(key → 파일) 저장소.
  - `DbMetadataStore`: JDBC/R2DBC 기반 테이블에 key/value/ttl 저장.
  - `KafkaKTableMetadataStore`: Kafka compacted topic + KTable(Streams)로 키-값 저장.
- 공통 요구사항:
  - `put/putIfAbsent/get/delete/compareAndSet/scan/watch` 준수.
  - TTL 지원(만료 시 삭제 이벤트).
  - 직렬화: JSON(기본), 필요 시 버전 필드 포함.

## 설정/선택
- `ees.metadatastore.backend`: `memory`(default) | `file` | `db` | `kafka-ktable`
- backend별 추가 설정:
  - `file`: `ees.metadatastore.file.path`
  - `db`: `ees.metadatastore.db.url`, `username`, `password`, `table`, `dialect`
  - `kafka-ktable`: `bootstrap-servers`, `topic`, `application-id`, `state-dir`

## 구현 단계
1) **SPI/팩토리**
   - `MetadataStoreFactory` 작성, backend 선택/환경 유효성 검증.
   - 직렬화/TTL 유틸 공통화.
2) **FileMetadataStore**
   - 키를 안전한 파일명으로 매핑, 디렉터리 구조 구성.
   - 만료 스캔/삭제 + watch 이벤트 발행.
3) **DbMetadataStore**
   - DDL 제안: `metadata(key PK, value JSON/BLOB, expires_at TIMESTAMP NULL)`.
   - JDBC/R2DBC 선택(우선 JDBC) + 만료 쿼리/정리 태스크.
4) **KafkaKTableMetadataStore**
   - Compacted topic에 JSON value로 저장.
   - Streams/KTable materialized view로 `get/scan` 구현, watch는 topic 구독으로 노출.
5) **테스트**
   - 공통 TCK-style 테스트 인터페이스로 각 backend 검증.
   - TTL/compareAndSet/watch 시나리오 포함.

## 클러스터/스냅샷 연계
- Raft 스냅샷이 `MetadataStore`를 snapshot 저장소로 사용할 수 있도록 key prefix(`cluster:raft/snapshots/{group}`) 표준화.
- backend가 file/db/kafka-ktable로 설정되면 스냅샷도 동일 backend에 저장/복원.

## 진행 현황 / 다음 액션
- [x] InMemory 구현을 blocking API로 정리하고 기본 TCK 테스트를 동기식으로 전환.
- [ ] `MetadataStoreFactory`/공통 직렬화·TTL 유틸을 추가해 backend 선택 진입점 마련.
- [ ] `FileMetadataStore` 구현 + 만료 스캐너/이벤트 발행.
- [ ] JDBC 기반 `DbMetadataStore` 구현 + 만료 정리 태스크.
- [ ] Kafka KTable backend 구현 + watch/scan 패턴 정리.
- [ ] 공통 TCK 테스트를 backend별로 재사용하고 Raft 스냅샷 키 표준 반영.
