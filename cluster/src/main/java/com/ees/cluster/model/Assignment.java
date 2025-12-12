package com.ees.cluster.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * 파티션(또는 그룹) 단위의 할당 결과를 표현한다.
 * <p>
 * affinities는 kind → 값 목록 구조로 유지되며, 기본 kind는 {@link AffinityKeys#DEFAULT}를 사용한다.
 *
 * @param groupId 할당 그룹 ID
 * @param partition 파티션 번호
 * @param ownerNodeId 소유 노드 ID
 * @param affinities affinity kind별 값 목록
 * @param workflowHandoff 워크플로 handoff 정보(옵션)
 * @param version 버전
 * @param updatedAt 갱신 시각
 */
public record Assignment(
        String groupId,
        int partition,
        String ownerNodeId,
        Map<String, List<String>> affinities,
        WorkflowHandoff workflowHandoff,
        long version,
        Instant updatedAt
) {

    /**
     * affinities 맵을 포함한 할당을 생성한다.
     */
    public Assignment(String groupId,
                      int partition,
                      String ownerNodeId,
                      Map<String, List<String>> affinities,
                      WorkflowHandoff workflowHandoff,
                      long version,
                      Instant updatedAt) {
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.partition = partition;
        this.ownerNodeId = Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        this.affinities = normalizeAffinities(Objects.requireNonNull(affinities, "affinities must not be null"));
        this.workflowHandoff = workflowHandoff;
        this.version = version;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * 기본 kind({@link AffinityKeys#DEFAULT})의 equipmentIds로 할당을 생성한다.
     */
    public Assignment(String groupId,
                      int partition,
                      String ownerNodeId,
                      List<String> equipmentIds,
                      WorkflowHandoff workflowHandoff,
                      long version,
                      Instant updatedAt) {
        this(groupId, partition, ownerNodeId,
                Map.of(DEFAULT, Collections.unmodifiableList(
                        List.copyOf(Objects.requireNonNull(equipmentIds, "equipmentIds must not be null")))),
                workflowHandoff, version, updatedAt);
    }

    /**
     * 기본 kind({@link AffinityKeys#DEFAULT})의 affinity 값 목록을 반환한다.
     *
     * @return equipmentId 목록
     */
    public List<String> equipmentIds() {
        return affinities.getOrDefault(DEFAULT, List.of());
    }

    /**
     * 주어진 kind에 대한 affinity 값 목록을 반환한다.
     *
     * @param kind affinity kind
     * @return 값 목록(없으면 빈 리스트)
     */
    public List<String> affinityValues(String kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return affinities.getOrDefault(kind, List.of());
    }

    /**
     * Jackson 역직렬화를 지원하는 팩토리 메서드.
     * <p>
     * affinities가 null이면 equipmentIds를 기반으로 기본 kind만 채운다.
     */
    @JsonCreator
    public static Assignment create(@JsonProperty("groupId") String groupId,
                                    @JsonProperty("partition") int partition,
                                    @JsonProperty("ownerNodeId") String ownerNodeId,
                                    @JsonProperty("affinities") Map<String, List<String>> affinities,
                                    @JsonProperty("workflowHandoff") WorkflowHandoff workflowHandoff,
                                    @JsonProperty("version") long version,
                                    @JsonProperty("updatedAt") Instant updatedAt,
                                    @JsonProperty("equipmentIds") List<String> equipmentIds) {
        Map<String, List<String>> resolvedAffinities = affinities != null
                ? affinities
                : equipmentIds != null
                    ? Map.of(DEFAULT, Collections.unmodifiableList(List.copyOf(equipmentIds)))
                    : Map.of();
        return new Assignment(groupId, partition, ownerNodeId, resolvedAffinities, workflowHandoff, version, updatedAt);
    }
    // normalizeAffinities 동작을 수행한다.

    private Map<String, List<String>> normalizeAffinities(Map<String, List<String>> affinities) {
        // kind/value 목록을 불변 구조로 정규화하고, null 요소를 방어한다.
        if (affinities.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new HashMap<>();
        affinities.forEach((kind, values) -> {
            Objects.requireNonNull(kind, "affinity kind must not be null");
            List<String> normalizedValues = Collections.unmodifiableList(List.copyOf(
                    Objects.requireNonNull(values, "affinity values must not be null for kind " + kind)));
            copy.put(kind, normalizedValues);
        });
        return Collections.unmodifiableMap(copy);
    }
}
