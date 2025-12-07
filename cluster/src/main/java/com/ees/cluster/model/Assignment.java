package com.ees.cluster.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record Assignment(
        String groupId,
        int partition,
        String ownerNodeId,
        List<String> equipmentIds,
        WorkflowHandoff workflowHandoff,
        long version,
        Instant updatedAt
) {

    public Assignment(String groupId,
                      int partition,
                      String ownerNodeId,
                      List<String> equipmentIds,
                      WorkflowHandoff workflowHandoff,
                      long version,
                      Instant updatedAt) {
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.partition = partition;
        this.ownerNodeId = Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        this.equipmentIds = Collections.unmodifiableList(List.copyOf(Objects.requireNonNull(equipmentIds, "equipmentIds must not be null")));
        this.workflowHandoff = workflowHandoff;
        this.version = version;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
