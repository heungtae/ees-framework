package com.ees.cluster.raft.command;

import java.util.Objects;

public record ReleaseLockCommand(
        String name,
        String ownerNodeId
) implements RaftCommand {

    public ReleaseLockCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
    }

    @Override
    public CommandType type() {
        return CommandType.LOCK_RELEASE;
    }
}
