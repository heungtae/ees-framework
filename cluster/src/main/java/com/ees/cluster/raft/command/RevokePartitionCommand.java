package com.ees.cluster.raft.command;

import java.util.Objects;

/**
 * 파티션 할당 해제를 기록하는 Raft 명령.
 *
 * @param groupId 그룹 ID
 * @param partition 파티션 번호
 * @param reason 해제 사유
 */
public record RevokePartitionCommand(
        String groupId,
        int partition,
        String reason
) implements RaftCommand {

    public RevokePartitionCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
    /**
     * type를 수행한다.
     * @return 
     */

    @Override
    public CommandType type() {
        return CommandType.REVOKE_PARTITION;
    }
}
