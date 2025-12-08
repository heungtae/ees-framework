package com.ees.cluster.spring;

import com.ees.cluster.raft.RaftStateMachineMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;
import java.util.Objects;

/**
 * Binds Raft state machine metrics to Micrometer if any metrics instances are available.
 */
public class RaftMetricsRegistrar implements MeterBinder {

    private final List<RaftStateMachineMetrics> metricsList;

    public RaftMetricsRegistrar(List<RaftStateMachineMetrics> metricsList) {
        this.metricsList = Objects.requireNonNull(metricsList, "metricsList must not be null");
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        if (metricsList.isEmpty()) {
            return;
        }
        for (RaftStateMachineMetrics metrics : metricsList) {
            var tags = io.micrometer.core.instrument.Tags.of("group", metrics.groupId());
            registry.gauge("ees.cluster.raft.applied.index", tags, metrics, RaftStateMachineMetrics::lastAppliedIndex);
            registry.gauge("ees.cluster.raft.snapshot.index", tags, metrics, RaftStateMachineMetrics::lastSnapshotIndex);
            registry.gauge("ees.cluster.raft.snapshot.count", tags, metrics, RaftStateMachineMetrics::snapshotsTaken);
            registry.gauge("ees.cluster.raft.running", tags, metrics, m -> m.running() ? 1 : 0);
            registry.gauge("ees.cluster.raft.safe_mode", tags, metrics, m -> m.safeMode() ? 1 : 0);
        }
    }
}
