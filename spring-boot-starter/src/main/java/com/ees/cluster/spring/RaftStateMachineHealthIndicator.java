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
    /**
     * 인스턴스를 생성한다.
     * @param metricsList 
     * @param clock 
     */

    public RaftStateMachineHealthIndicator(List<RaftStateMachineMetrics> metricsList, Clock clock) {
        this.metricsList = metricsList;
        this.clock = clock;
    }
    /**
     * health를 수행한다.
     * @return 
     */

    @Override
    public Health health() {
        if (metricsList.isEmpty()) {
            return Health.unknown().withDetail("raft", "no-state-machines").build();
        }
        boolean allRunning = true;
        Health.Builder details = Health.up();
        for (RaftStateMachineMetrics metrics : metricsList) {
            RaftHealthSnapshot snapshot = metrics.health(clock);
            details.withDetail(snapshot.groupId() + ".leader", snapshot.leaderId());
            details.withDetail(snapshot.groupId() + ".appliedIndex", snapshot.lastAppliedIndex());
            details.withDetail(snapshot.groupId() + ".snapshotIndex", snapshot.lastSnapshotIndex());
            details.withDetail(snapshot.groupId() + ".stale", snapshot.stale());
            details.withDetail(snapshot.groupId() + ".safeMode", snapshot.safeMode());
            details.withDetail(snapshot.groupId() + ".safeModeReason", snapshot.safeModeReason());
            if (!snapshot.running() || snapshot.stale() || snapshot.safeMode()) {
                allRunning = false;
            }
        }
        Health.Builder state = allRunning ? Health.up() : Health.outOfService();
        return state.withDetails(details.build().getDetails()).build();
    }
}
