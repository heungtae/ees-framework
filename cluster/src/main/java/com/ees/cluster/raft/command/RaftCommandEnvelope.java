package com.ees.cluster.raft.command;

import java.util.Objects;

public record RaftCommandEnvelope(
        long version,
        CommandType type,
        RaftCommand command
) {

    public RaftCommandEnvelope {
        if (version <= 0) {
            throw new IllegalArgumentException("version must be > 0");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(command, "command must not be null");
        if (command.type() != type) {
            throw new IllegalArgumentException("command type " + command.type() + " does not match envelope type " + type);
        }
    }
}
