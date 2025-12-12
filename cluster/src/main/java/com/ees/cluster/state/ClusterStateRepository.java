package com.ees.cluster.state;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 클러스터 상태(메타데이터)를 저장/조회하고 변경 이벤트를 구독할 수 있는 저장소 추상화.
 * <p>
 * TTL 및 prefix 기반 watch를 제공하며, 백엔드 구현체는 in-memory, metadata-store 위임 등으로 확장될 수 있다.
 */
public interface ClusterStateRepository {

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
     * @return 저장 성공 시 true, 이미 값이 있으면 false
     * @param <T> 값 타입
     */
    <T> boolean putIfAbsent(String key, T value, Duration ttl);

    /**
     * 주어진 키의 값을 조회한다.
     *
     * @param key 조회할 키(널 불가)
     * @param type 기대하는 타입(널 불가)
     * @return 값(없거나 타입 불일치면 empty)
     * @param <T> 값 타입
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 주어진 키를 삭제한다.
     *
     * @param key 삭제할 키(널 불가)
     * @return 삭제되면 true
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
     * prefix로 시작하는 키들의 값을 스캔한다.
     *
     * @param prefix 키 prefix(널 불가)
     * @param type 기대하는 타입(널 불가)
     * @return 결과 목록
     * @param <T> 값 타입
     */
    <T> List<T> scan(String prefix, Class<T> type);

    /**
     * prefix로 시작하는 키에 대한 변경 이벤트를 구독한다.
     *
     * @param prefix 구독 prefix(널 불가)
     * @param consumer 이벤트 소비자(널 불가)
     */
    void watch(String prefix, Consumer<ClusterStateEvent> consumer);
}
