package com.ees.cluster.raft.command;

import com.ees.cluster.model.WorkflowHandoff;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record AssignPartitionCommand(
        String groupId,
        int partition,
        String ownerNodeId,
        List<String> equipmentIds,
        WorkflowHandoff workflowHandoff
) implements RaftCommand {

    public AssignPartitionCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        equipmentIds = Collections.unmodifiableList(List.copyOf(Objects.requireNonNull(equipmentIds, "equipmentIds must not be null")));
    }

    @Override
    public CommandType type() {
        return CommandType.ASSIGN_PARTITION;
    }
}
