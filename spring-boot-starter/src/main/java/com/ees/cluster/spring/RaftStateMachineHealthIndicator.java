package com.ees.cluster.spring;

import com.ees.cluster.raft.RaftHealthSnapshot;
import com.ees.cluster.raft.RaftStateMachineMetrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.Clock;
import java.util.List;

/**
 * Aggregates Raft state machine health into a Spring Boot health indicator.
 */
public class RaftStateMachineHealthIndicator implements HealthIndicator {

    private final List<RaftStateMachineMetrics> metricsList;
    private final Clock clock;

    public RaftStateMachineHealthIndicator(List<RaftStateMachineMetrics> metricsList, Clock clock) {
        this.metricsList = metricsList;
        this.clock = clock;
    }

    @Override
    public Health health() {
        if (metricsList.isEmpty()) {
            return Health.unknown().withDetail("raft", "no-state-machines").build();
        }
        boolean allRunning = true;
        Health.Builder builder = Health.up();
        for (RaftStateMachineMetrics metrics : metricsList) {
            RaftHealthSnapshot snapshot = metrics.health(clock);
            builder.withDetail(snapshot.groupId() + ".leader", snapshot.leaderId());
            builder.withDetail(snapshot.groupId() + ".appliedIndex", snapshot.lastAppliedIndex());
            builder.withDetail(snapshot.groupId() + ".snapshotIndex", snapshot.lastSnapshotIndex());
            builder.withDetail(snapshot.groupId() + ".stale", snapshot.stale());
            if (!snapshot.running() || snapshot.stale()) {
                allRunning = false;
            }
        }
        return allRunning ? builder.build() : Health.down().withDetails(builder.build().getDetails()).build();
    }
}
