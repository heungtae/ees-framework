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
    // ObjectMapper 동작을 수행한다.

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.findAndRegisterModules();
    }
    // 인스턴스를 생성한다.

    private ClusterSnapshotCodec() {
    }
    /**
     * serialize를 수행한다.
     * @param snapshot 
     * @return 
     */

    public static byte[] serialize(ClusterSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        try {
            return mapper.writeValueAsBytes(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize snapshot", e);
        }
    }
    /**
     * deserialize를 수행한다.
     * @param data 
     * @return 
     */

    public static ClusterSnapshot deserialize(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data must not be null");
        return mapper.readValue(data, ClusterSnapshot.class);
    }
}
