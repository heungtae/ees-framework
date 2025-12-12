package com.ees.metadatastore;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * JSON-based serializer for metadata values.
 */
public class JsonMetadataSerializer implements MetadataSerializer {

    private final ObjectMapper objectMapper;

    public JsonMetadataSerializer() {
        this(new ObjectMapper());
    }

    public JsonMetadataSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public byte[] serialize(Object value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize metadata value", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] payload, Class<T> type) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(type, "type must not be null");
        try {
            return objectMapper.readValue(payload, type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize metadata value", e);
        }
    }
}
