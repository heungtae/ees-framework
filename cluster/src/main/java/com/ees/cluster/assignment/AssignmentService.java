package com.ees.cluster.assignment;

import com.ees.cluster.model.Assignment;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.TopologyEvent;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * 파티션/키 할당(assignment)을 관리하고 토폴로지 이벤트를 발행하는 서비스.
 * <p>
 * 워크플로 per-key 실행/라우팅을 위해 "어떤 키가 어떤 노드에 할당되는지"를 결정/저장하고,
 * 변경을 {@link TopologyEvent}로 구독자에게 전달한다.
 */
public interface AssignmentService {

    String DEFAULT_AFFINITY_KIND = DEFAULT;

    /**
     * 파티션 단위 할당을 적용한다(추가/갱신).
     *
     * @param groupId 그룹 ID
     * @param assignments 할당 목록
     */
    void applyAssignments(String groupId, Collection<Assignment> assignments);

    /**
     * 파티션 단위 할당을 해제한다.
     *
     * @param groupId 그룹 ID
     * @param partitions 해제할 파티션 목록
     * @param reason 해제 사유
     */
    void revokeAssignments(String groupId, Collection<Integer> partitions, String reason);

    /**
     * 특정 파티션의 할당을 조회한다.
     *
     * @param groupId 그룹 ID
     * @param partition 파티션 번호
     * @return 할당(없으면 empty)
     */
    Optional<Assignment> findAssignment(String groupId, int partition);

    /**
     * 기본 affinity kind를 사용해 키를 할당한다.
     */
    default KeyAssignment assignKey(String groupId, int partition, String key, String appId, KeyAssignmentSource source) {
        return assignKey(groupId, partition, DEFAULT_AFFINITY_KIND, key, appId, source);
    }

    /**
     * 레코드에서 키를 추출해 키 할당을 수행한다.
     *
     * @param groupId 그룹 ID
     * @param partition 파티션 번호
     * @param record 입력 레코드
     * @param extractor 키/affinity 정보를 추출하는 함수
     * @param appId 앱/워크플로 식별자
     * @param source 할당 출처
     * @return 키가 비어있으면 empty, 아니면 할당 결과
     * @param <T> 입력 레코드 타입
     */
    default <T> Optional<KeyAssignment> assignKey(String groupId,
                                                  int partition,
                                                  T record,
                                                  AffinityKeyExtractor<T> extractor,
                                                  String appId,
                                                  KeyAssignmentSource source) {
        Objects.requireNonNull(extractor, "extractor must not be null");
        String key = extractor.extract(record);
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assignKey(groupId, partition, extractor.kind(), key, appId, source));
    }

    /**
     * 키를 할당한다.
     *
     * @param groupId 그룹 ID
     * @param partition 파티션 번호
     * @param kind affinity kind
     * @param key 대상 키
     * @param appId 앱/워크플로 식별자
     * @param source 할당 출처
     * @return 키 할당 결과
     */
    KeyAssignment assignKey(String groupId, int partition, String kind, String key, String appId, KeyAssignmentSource source);

    /**
     * 기본 affinity kind를 사용해 키 할당을 조회한다.
     */
    default Optional<KeyAssignment> getKeyAssignment(String groupId, int partition, String key) {
        return getKeyAssignment(groupId, partition, DEFAULT_AFFINITY_KIND, key);
    }

    /**
     * 키 할당을 조회한다.
     */
    Optional<KeyAssignment> getKeyAssignment(String groupId, int partition, String kind, String key);

    /**
     * 기본 affinity kind를 사용해 키 할당을 해제한다.
     */
    default boolean unassignKey(String groupId, int partition, String key) {
        return unassignKey(groupId, partition, DEFAULT_AFFINITY_KIND, key);
    }

    /**
     * 키 할당을 해제한다.
     *
     * @return 해제되었으면 true
     */
    boolean unassignKey(String groupId, int partition, String kind, String key);

    /**
     * 토폴로지 이벤트를 구독한다.
     *
     * @param consumer 이벤트 소비자
     */
    void topologyEvents(Consumer<TopologyEvent> consumer);
}
