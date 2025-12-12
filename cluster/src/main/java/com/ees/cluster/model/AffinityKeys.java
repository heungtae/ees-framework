package com.ees.cluster.model;

/**
 * Common affinity kinds used to shard partitions/raft groups.
 */
public final class AffinityKeys {

    public static final String DEFAULT = "equipmentId";
    // 인스턴스를 생성한다.

    private AffinityKeys() {
    }
}
