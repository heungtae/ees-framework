package com.ees.cluster.raft.command;

import com.ees.cluster.model.KeyAssignmentSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * 키 단위 할당을 기록하는 Raft 명령.
 *
 * @param groupId 그룹 ID
 * @param partition 파티션 번호
 * @param kind affinity kind(null이면 DEFAULT로 보정)
 * @param key 대상 키
 * @param appId 앱/워크플로 식별자
 * @param source 할당 출처
 */
public record AssignKeyCommand(
        String groupId,
        int partition,
        String kind,
        String key,
        String appId,
        KeyAssignmentSource source
) implements RaftCommand {

    public AssignKeyCommand {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }

    /**
     * Jackson 역직렬화를 지원하는 팩토리 메서드.
     * <p>
     * kind가 null이면 {@code DEFAULT}로 보정한다.
     */
    @JsonCreator
    public static AssignKeyCommand create(@JsonProperty("groupId") String groupId,
                                          @JsonProperty("partition") int partition,
                                          @JsonProperty("kind") String kind,
                                          @JsonProperty("key") String key,
                                          @JsonProperty("appId") String appId,
                                          @JsonProperty("source") KeyAssignmentSource source) {
        String resolvedKind = kind != null ? kind : DEFAULT;
        return new AssignKeyCommand(groupId, partition, resolvedKind, key, appId, source);
    }
    /**
     * type를 수행한다.
     * @return 
     */

    @Override
    public CommandType type() {
        return CommandType.ASSIGN_KEY;
    }
}
