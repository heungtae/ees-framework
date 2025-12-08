package com.ees.cluster.raft.command;

import java.util.Objects;

public record RevokePartitionCommand(
        String groupId,
        int partition,
        String reason
) implements RaftCommand {

    public RevokePartitionCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }

    @Override
    public CommandType type() {
        return CommandType.REVOKE_PARTITION;
    }
}
