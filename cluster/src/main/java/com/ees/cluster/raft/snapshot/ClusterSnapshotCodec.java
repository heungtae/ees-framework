package com.ees.cluster.raft.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * JSON codec for {@link ClusterSnapshot}. Stored blobs include a format version so future migrations
 * can be handled gracefully.
 */
public final class ClusterSnapshotCodec {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.findAndRegisterModules();
    }

    private ClusterSnapshotCodec() {
    }

    public static byte[] serialize(ClusterSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        try {
            return mapper.writeValueAsBytes(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize snapshot", e);
        }
    }

    public static ClusterSnapshot deserialize(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data must not be null");
        return mapper.readValue(data, ClusterSnapshot.class);
    }
}
