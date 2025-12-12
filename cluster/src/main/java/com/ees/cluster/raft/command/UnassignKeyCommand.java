package com.ees.cluster.raft.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * 키 할당 해제를 기록하는 Raft 명령.
 *
 * @param groupId 그룹 ID
 * @param partition 파티션 번호
 * @param kind affinity kind(null이면 DEFAULT로 보정)
 * @param key 대상 키
 */
public record UnassignKeyCommand(
        String groupId,
        int partition,
        String kind,
        String key
) implements RaftCommand {

    public UnassignKeyCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
    }

    /**
     * Jackson 역직렬화를 지원하는 팩토리 메서드.
     * <p>
     * kind가 null이면 {@code DEFAULT}로 보정한다.
     */
    @JsonCreator
    public static UnassignKeyCommand create(@JsonProperty("groupId") String groupId,
                                            @JsonProperty("partition") int partition,
                                            @JsonProperty("kind") String kind,
                                            @JsonProperty("key") String key) {
        String resolvedKind = kind != null ? kind : DEFAULT;
        return new UnassignKeyCommand(groupId, partition, resolvedKind, key);
    }
    /**
     * type를 수행한다.
     * @return 
     */

    @Override
    public CommandType type() {
        return CommandType.UNASSIGN_KEY;
    }
}
