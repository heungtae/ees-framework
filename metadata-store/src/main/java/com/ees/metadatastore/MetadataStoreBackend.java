package com.ees.metadatastore;

/**
 * Supported metadata store backend types.
 */
public enum MetadataStoreBackend {
    MEMORY,
    FILE,
    DB,
    KAFKA_KTABLE
}
