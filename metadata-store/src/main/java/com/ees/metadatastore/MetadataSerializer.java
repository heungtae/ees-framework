package com.ees.metadatastore;

/**
 * Serializer for persisting metadata values.
 */
public interface MetadataSerializer {

    byte[] serialize(Object value);

    <T> T deserialize(byte[] payload, Class<T> type);
}
