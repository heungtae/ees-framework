package com.ees.framework.metadatastore;

/**
 * Framework 전체에서 사용하는 메타데이터 저장소 인터페이스.
 *
 * 구현 예:
 * - Raft 모드: snapshot 파일 기반
 * - Kafka 모드: Kafka Streams KTable 기반
 */
public interface MetadataStore {

    void save(String key, byte[] value);

    byte[] find(String key);

    java.util.List<byte[]> findAll();
}
