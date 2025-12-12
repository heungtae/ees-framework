package com.ees.cluster.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 리더 선출 결과 및 리스(lease) 정보를 담는 모델.
 *
 * @param groupId 리더 그룹 ID
 * @param leaderNodeId 리더 노드 ID
 * @param mode 선출 방식
 * @param term 선출 텀(term)
 * @param electedAt 선출 시각
 * @param leaseExpiresAt 리스 만료 시각
 */
public record LeaderInfo(
        String groupId,
        String leaderNodeId,
        LeaderElectionMode mode,
        long term,
        Instant electedAt,
        Instant leaseExpiresAt
) {

    public LeaderInfo {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(leaderNodeId, "leaderNodeId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(electedAt, "electedAt must not be null");
        Objects.requireNonNull(leaseExpiresAt, "leaseExpiresAt must not be null");
    }
}
