package com.ees.cluster.raft.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * {@link RaftCommandEnvelope}을 JSON으로 직렬화/역직렬화하는 코덱.
 */
public final class RaftCommandCodec {
    // ObjectMapper 동작을 수행한다.

    private static final ObjectMapper mapper = new ObjectMapper();
    // 인스턴스를 생성한다.

    private RaftCommandCodec() {
    }

    static {
        mapper.findAndRegisterModules();
    }
    /**
     * serialize를 수행한다.
     * @param envelope 
     * @return 
     */

    public static byte[] serialize(RaftCommandEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        try {
            return mapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize raft command", e);
        }
    }
    /**
     * deserialize를 수행한다.
     * @param data 
     * @return 
     */

    public static RaftCommandEnvelope deserialize(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data must not be null");
        JsonNode node = mapper.readTree(data);
        if (node == null) {
            throw new IOException("Empty raft command payload");
        }
        long version = node.path("version").asLong(1);
        CommandType type = CommandType.valueOf(node.path("type").asText());
        JsonNode payload = node.get("command");
        if (payload == null) {
            throw new IOException("Missing command payload");
        }
        RaftCommand command = decodePayload(type, payload);
        return new RaftCommandEnvelope(version, type, command);
    }
    // decodePayload 동작을 수행한다.

    private static RaftCommand decodePayload(CommandType type, JsonNode payload) throws JsonProcessingException {
        // CommandType에 따라 payload를 구체 명령 클래스로 역직렬화한다.
        return switch (type) {
            case LOCK_ACQUIRE -> mapper.treeToValue(payload, LockCommand.class);
            case LOCK_RELEASE -> mapper.treeToValue(payload, ReleaseLockCommand.class);
            case ASSIGN_PARTITION -> mapper.treeToValue(payload, AssignPartitionCommand.class);
            case REVOKE_PARTITION -> mapper.treeToValue(payload, RevokePartitionCommand.class);
            case ASSIGN_KEY -> mapper.treeToValue(payload, AssignKeyCommand.class);
            case UNASSIGN_KEY -> mapper.treeToValue(payload, UnassignKeyCommand.class);
        };
    }
}
