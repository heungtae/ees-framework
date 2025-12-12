package com.ees.metadatastore;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * TTL(만료)과 이벤트 watch를 지원하는 간단한 Key-Value 메타데이터 저장소.
 * <p>
 * 구현체는 값 직렬화 방식/백엔드(JDBC, Kafka, 파일 등)를 자유롭게 선택할 수 있으며,
 * 본 인터페이스는 워크플로/클러스터 컴포넌트에서 사용할 최소 기능 집합을 정의한다.
 */
public interface MetadataStore {

    /**
     * 주어진 키에 값을 저장한다.
     *
     * @param key 저장할 키(널 불가)
     * @param value 저장할 값(널 불가)
     * @param ttl 만료 시간(널 불가)
     * @return 저장 성공 여부
     * @param <T> 값 타입
     */
    <T> boolean put(String key, T value, Duration ttl);

    /**
     * 주어진 키가 존재하지 않을 때만 값을 저장한다.
     *
     * @param key 저장할 키(널 불가)
     * @param value 저장할 값(널 불가)
     * @param ttl 만료 시간(널 불가)
     * @return 저장에 성공하면 true, 이미 값이 존재하면 false
     * @param <T> 값 타입
     */
    <T> boolean putIfAbsent(String key, T value, Duration ttl);

    /**
     * 주어진 키의 값을 조회한다.
     * <p>
     * 키가 없거나 TTL이 만료되었거나 타입이 일치하지 않으면 빈 값을 반환한다.
     *
     * @param key 조회할 키(널 불가)
     * @param type 기대하는 타입(널 불가)
     * @return 조회된 값(없으면 empty)
     * @param <T> 값 타입
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 주어진 키의 값을 삭제한다.
     *
     * @param key 삭제할 키(널 불가)
     * @return 삭제되었으면 true, 키가 없으면 false
     */
    boolean delete(String key);

    /**
     * 현재 값이 기대 값과 같을 때만 새 값으로 교체한다(CAS).
     *
     * @param key 대상 키(널 불가)
     * @param expectedValue 기대 값(널 불가)
     * @param newValue 새 값(널 불가)
     * @param ttl 새 값에 적용할 TTL(널 불가)
     * @return 교체 성공 여부
     * @param <T> 값 타입
     */
    <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl);

    /**
     * 특정 prefix로 시작하는 키들의 값을 스캔한다.
     *
     * @param prefix 키 prefix(널 불가)
     * @param type 기대하는 타입(널 불가)
     * @return 스캔 결과 목록(타입이 일치하는 항목만 포함)
     * @param <T> 값 타입
     */
    <T> List<T> scan(String prefix, Class<T> type);

    /**
     * 특정 prefix로 시작하는 키 변경 이벤트를 구독한다.
     *
     * @param prefix 구독할 prefix(널 불가)
     * @param consumer 이벤트 소비자(널 불가)
     */
    void watch(String prefix, Consumer<MetadataStoreEvent> consumer);
}
