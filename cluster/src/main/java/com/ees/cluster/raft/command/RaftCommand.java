package com.ees.cluster.raft.command;

/**
 * Raft 로그에 기록되는 명령의 공통 인터페이스.
 */
public sealed interface RaftCommand permits LockCommand, ReleaseLockCommand,
        AssignPartitionCommand, RevokePartitionCommand, AssignKeyCommand, UnassignKeyCommand {

    /**
     * 명령 타입을 반환한다.
     *
     * @return 명령 타입
     */
    CommandType type();
}
