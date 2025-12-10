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

public record Assignment(
        String groupId,
        int partition,
        String ownerNodeId,
        Map<String, List<String>> affinities,
        WorkflowHandoff workflowHandoff,
        long version,
        Instant updatedAt
) {

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

    public List<String> equipmentIds() {
        return affinities.getOrDefault(DEFAULT, List.of());
    }

    public List<String> affinityValues(String kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return affinities.getOrDefault(kind, List.of());
    }

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

    private Map<String, List<String>> normalizeAffinities(Map<String, List<String>> affinities) {
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
