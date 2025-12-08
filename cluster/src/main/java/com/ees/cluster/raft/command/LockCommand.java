package com.ees.cluster.raft.command;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record LockCommand(
        String name,
        String ownerNodeId,
        long leaseMillis,
        Map<String, String> metadata
) implements RaftCommand {

    public LockCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        if (leaseMillis <= 0) {
            throw new IllegalArgumentException("leaseMillis must be > 0");
        }
        metadata = Collections.unmodifiableMap(Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null")));
    }

    @Override
    public CommandType type() {
        return CommandType.LOCK_ACQUIRE;
    }
}
