package com.ees.cluster.model;

/**
 * Common affinity kinds used to shard partitions/raft groups.
 */
public final class AffinityKeys {

    public static final String DEFAULT = "equipmentId";

    private AffinityKeys() {
    }
}
