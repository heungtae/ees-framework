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

    public List<String> equipmentIds() {
        return affinities.getOrDefault(DEFAULT, List.of());
    }

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

    @Override
    public CommandType type() {
        return CommandType.ASSIGN_PARTITION;
    }

    private Map<String, List<String>> normalizeAffinities(Map<String, List<String>> affinities) {
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
