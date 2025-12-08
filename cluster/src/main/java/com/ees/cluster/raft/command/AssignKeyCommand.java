package com.ees.cluster.raft.command;

import com.ees.cluster.model.KeyAssignmentSource;

import java.util.Objects;

public record AssignKeyCommand(
        String groupId,
        int partition,
        String key,
        String appId,
        KeyAssignmentSource source
) implements RaftCommand {

    public AssignKeyCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }

    @Override
    public CommandType type() {
        return CommandType.ASSIGN_KEY;
    }
}
