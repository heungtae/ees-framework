package com.ees.cluster.raft.command;

import java.util.Objects;

public record UnassignKeyCommand(
        String groupId,
        int partition,
        String key
) implements RaftCommand {

    public UnassignKeyCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(key, "key must not be null");
    }

    @Override
    public CommandType type() {
        return CommandType.UNASSIGN_KEY;
    }
}
