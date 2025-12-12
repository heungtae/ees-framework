package com.ees.cluster.raft.command;

import java.util.Objects;

/**
 * 락 해제를 기록하는 Raft 명령.
 *
 * @param name 락 이름
 * @param ownerNodeId 소유 노드 ID
 */
public record ReleaseLockCommand(
        String name,
        String ownerNodeId
) implements RaftCommand {

    public ReleaseLockCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
    }
    /**
     * type를 수행한다.
     * @return 
     */

    @Override
    public CommandType type() {
        return CommandType.LOCK_RELEASE;
    }
}
