package com.ees.cluster.membership;

import java.time.Duration;
import java.util.Objects;

public class ClusterMembershipProperties {

    private final Duration heartbeatInterval;
    private final Duration heartbeatTimeout;

    public ClusterMembershipProperties(Duration heartbeatInterval, Duration heartbeatTimeout) {
        this.heartbeatInterval = Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
        this.heartbeatTimeout = Objects.requireNonNull(heartbeatTimeout, "heartbeatTimeout must not be null");
    }

    public static ClusterMembershipProperties defaults() {
        return new ClusterMembershipProperties(Duration.ofSeconds(5), Duration.ofSeconds(15));
    }

    public Duration heartbeatInterval() {
        return heartbeatInterval;
    }

    public Duration heartbeatTimeout() {
        return heartbeatTimeout;
    }
}
