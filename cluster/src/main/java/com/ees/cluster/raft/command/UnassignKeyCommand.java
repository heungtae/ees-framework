package com.ees.cluster.raft.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static com.ees.cluster.model.AffinityKeys.DEFAULT;

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

    @JsonCreator
    public static UnassignKeyCommand create(@JsonProperty("groupId") String groupId,
                                            @JsonProperty("partition") int partition,
                                            @JsonProperty("kind") String kind,
                                            @JsonProperty("key") String key) {
        String resolvedKind = kind != null ? kind : DEFAULT;
        return new UnassignKeyCommand(groupId, partition, resolvedKind, key);
    }

    @Override
    public CommandType type() {
        return CommandType.UNASSIGN_KEY;
    }
}
