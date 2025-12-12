package com.ees.cluster.raft.command;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 락 획득을 기록하는 Raft 명령.
 *
 * @param name 락 이름
 * @param ownerNodeId 소유 노드 ID
 * @param leaseMillis 리스 기간(ms, 0 초과)
 * @param metadata 추가 메타데이터
 */
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
    /**
     * type를 수행한다.
     * @return 
     */

    @Override
    public CommandType type() {
        return CommandType.LOCK_ACQUIRE;
    }
}
