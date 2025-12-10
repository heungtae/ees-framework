package com.ees.cluster.raft.command;

import com.ees.cluster.model.KeyAssignmentSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

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

    @Override
    public CommandType type() {
        return CommandType.ASSIGN_KEY;
    }
}
