package com.ees.cluster.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

/**
 * 특정 키에 대한 할당(ownership) 정보를 표현한다.
 *
 * @param groupId 할당 그룹 ID(널 불가)
 * @param partition 파티션 번호
 * @param kind affinity kind(널 불가, null이면 DEFAULT로 보정)
 * @param key 할당 대상 키(널 불가)
 * @param appId 앱/워크플로 식별자(널 불가)
 * @param assignedBy 할당 출처(널 불가)
 * @param version 버전
 * @param updatedAt 갱신 시각(널 불가)
 */
public record KeyAssignment(
        String groupId,
        int partition,
        String kind,
        String key,
        String appId,
        KeyAssignmentSource assignedBy,
        long version,
        Instant updatedAt
) {

    public KeyAssignment {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        Objects.requireNonNull(assignedBy, "assignedBy must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Jackson 역직렬화를 지원하는 팩토리 메서드.
     * <p>
     * kind가 null이면 {@link AffinityKeys#DEFAULT}로 보정한다.
     */
    @JsonCreator
    public static KeyAssignment create(@JsonProperty("groupId") String groupId,
                                       @JsonProperty("partition") int partition,
                                       @JsonProperty("kind") String kind,
                                       @JsonProperty("key") String key,
                                       @JsonProperty("appId") String appId,
                                       @JsonProperty("assignedBy") KeyAssignmentSource assignedBy,
                                       @JsonProperty("version") long version,
                                       @JsonProperty("updatedAt") Instant updatedAt) {
        String resolvedKind = kind != null ? kind : DEFAULT;
        return new KeyAssignment(groupId, partition, resolvedKind, key, appId, assignedBy, version, updatedAt);
    }
}
