package com.ees.cluster.raft;

/**
 * Raft 서버의 간단한 상태(헬스) 요약.
 *
 * @param running 실행 중 여부
 * @param registeredGroups 등록된 그룹 수
 * @param registeredStateMachines 등록된 상태머신 수
 */
public record RaftServerHealth(
        boolean running,
        int registeredGroups,
        int registeredStateMachines
) {
}
