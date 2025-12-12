package com.ees.cluster.raft;

import java.time.Instant;
import java.util.Objects;

/**
 * Raft 그룹 단위 헬스 스냅샷.
 *
 * @param groupId 그룹 ID(널 불가)
 * @param running 실행 여부
 * @param leaderId 리더 peer ID(옵션)
 * @param lastAppliedIndex 마지막 적용 인덱스
 * @param lastSnapshotIndex 마지막 스냅샷 인덱스
 * @param lastAppliedAt 마지막 적용 시각(옵션)
 * @param lastSnapshotAt 마지막 스냅샷 시각(옵션)
 * @param stale 오래된 상태로 판단되는지 여부
 * @param safeMode 안전 모드 여부
 * @param safeModeReason 안전 모드 사유(옵션)
 */
public record RaftHealthSnapshot(
        String groupId,
        boolean running,
        String leaderId,
        long lastAppliedIndex,
        long lastSnapshotIndex,
        Instant lastAppliedAt,
        Instant lastSnapshotAt,
        boolean stale,
        boolean safeMode,
        String safeModeReason
) {

    public RaftHealthSnapshot {
        Objects.requireNonNull(groupId, "groupId must not be null");
    }
}
