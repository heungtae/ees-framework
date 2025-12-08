package com.ees.cluster.raft.command;

public sealed interface RaftCommand permits LockCommand, ReleaseLockCommand,
        AssignPartitionCommand, RevokePartitionCommand, AssignKeyCommand, UnassignKeyCommand {
    CommandType type();
}
