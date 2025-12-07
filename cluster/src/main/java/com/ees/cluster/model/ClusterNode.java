package com.ees.cluster.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ClusterNode(
        String nodeId,
        String host,
        int port,
        Set<ClusterRole> roles,
        String zone,
        Map<String, String> metadata,
        String version
) {

    public ClusterNode(String nodeId,
                       String host,
                       int port,
                       Set<ClusterRole> roles,
                       String zone,
                       Map<String, String> metadata,
                       String version) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.port = validatePort(port);
        this.roles = Collections.unmodifiableSet(Set.copyOf(Objects.requireNonNull(roles, "roles must not be null")));
        this.zone = zone;
        this.metadata = Collections.unmodifiableMap(Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null")));
        this.version = version;
    }

    private static int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        return port;
    }
}
