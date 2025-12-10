package com.ees.cluster.assignment;

/**
 * Strategy to extract a partitioning key and its kind (e.g., equipmentId, lotId) from a record or message.
 */
public interface AffinityKeyExtractor<T> {

    /**
     * Kind of the affinity key (equipmentId, lotId, etc).
     */
    String kind();

    /**
     * Extract the key value; may return null if not present.
     */
    String extract(T record);
}
