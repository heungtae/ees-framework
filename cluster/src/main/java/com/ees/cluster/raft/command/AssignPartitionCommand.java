package com.ees.cluster.raft.command;

import com.ees.cluster.model.WorkflowHandoff;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * 파티션 단위 할당을 기록하는 Raft 명령.
 *
 * @param groupId 그룹 ID
 * @param partition 파티션 번호
 * @param ownerNodeId 소유 노드 ID
 * @param affinities affinity kind별 값 목록
 * @param workflowHandoff handoff 정보(옵션)
 */
public record AssignPartitionCommand(
        String groupId,
        int partition,
        String ownerNodeId,
        Map<String, List<String>> affinities,
        WorkflowHandoff workflowHandoff
) implements RaftCommand {

    public AssignPartitionCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        affinities = normalizeAffinities(Objects.requireNonNull(affinities, "affinities must not be null"));
    }

    /**
     * 기본 kind의 equipmentIds로 명령을 생성한다.
     */
    public AssignPartitionCommand(String groupId,
                                  int partition,
                                  String ownerNodeId,
                                  List<String> equipmentIds,
                                  WorkflowHandoff workflowHandoff) {
        this(groupId, partition, ownerNodeId,
                Map.of(DEFAULT, Collections.unmodifiableList(
                        List.copyOf(Objects.requireNonNull(equipmentIds, "equipmentIds must not be null")))),
                workflowHandoff);
    }

    /**
     * 기본 kind({@code DEFAULT})의 equipmentIds 목록을 반환한다.
     */
    public List<String> equipmentIds() {
        return affinities.getOrDefault(DEFAULT, List.of());
    }

    /**
     * Jackson 역직렬화를 지원하는 팩토리 메서드.
     * <p>
     * affinities가 null이면 equipmentIds로 기본 kind를 채운다.
     */
    @JsonCreator
    public static AssignPartitionCommand create(@JsonProperty("groupId") String groupId,
                                                @JsonProperty("partition") int partition,
                                                @JsonProperty("ownerNodeId") String ownerNodeId,
                                                @JsonProperty("affinities") Map<String, List<String>> affinities,
                                                @JsonProperty("workflowHandoff") WorkflowHandoff workflowHandoff,
                                                @JsonProperty("equipmentIds") List<String> equipmentIds) {
        Map<String, List<String>> resolvedAffinities = affinities != null
                ? affinities
                : equipmentIds != null
                    ? Map.of(DEFAULT, Collections.unmodifiableList(List.copyOf(equipmentIds)))
                    : Map.of();
        return new AssignPartitionCommand(groupId, partition, ownerNodeId, resolvedAffinities, workflowHandoff);
    }
    /**
     * type를 수행한다.
     * @return 
     */

    @Override
    public CommandType type() {
        return CommandType.ASSIGN_PARTITION;
    }
    // normalizeAffinities 동작을 수행한다.

    private Map<String, List<String>> normalizeAffinities(Map<String, List<String>> affinities) {
        // kind/value 목록을 불변 구조로 정규화하고, null 요소를 방어한다.
        if (affinities.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> normalized = new HashMap<>();
        affinities.forEach((kind, values) -> {
            Objects.requireNonNull(kind, "affinity kind must not be null");
            List<String> normalizedValues = Collections.unmodifiableList(List.copyOf(
                    Objects.requireNonNull(values, "affinity values must not be null for kind " + kind)));
            normalized.put(kind, normalizedValues);
        });
        return Collections.unmodifiableMap(normalized);
    }
}
