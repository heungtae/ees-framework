package com.ees.cluster.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 분산 락 상태를 표현하는 모델.
 *
 * @param name 락 이름(널 불가)
 * @param ownerNodeId 소유 노드 ID(널 불가)
 * @param leaseUntil 리스 만료 시각(널 불가)
 * @param metadata 추가 메타데이터(널 불가)
 */
public record LockRecord(
        String name,
        String ownerNodeId,
        Instant leaseUntil,
        Map<String, String> metadata
) {

    public LockRecord {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(ownerNodeId, "ownerNodeId must not be null");
        Objects.requireNonNull(leaseUntil, "leaseUntil must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        metadata = Collections.unmodifiableMap(Map.copyOf(metadata));
    }

    /**
     * 주어진 시각 기준으로 리스가 만료되었는지 확인한다.
     *
     * @param now 비교 시각(널 불가)
     * @return 만료되었으면 true
     */
    public boolean isExpired(Instant now) {
        return leaseUntil.isBefore(now) || leaseUntil.equals(now);
    }
}
