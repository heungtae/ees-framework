package com.ees.cluster.spring;

import com.ees.cluster.assignment.ClusterAffinityKindMonitor;
import com.ees.cluster.assignment.InMemoryAssignmentService;
import com.ees.cluster.model.Assignment;
import com.ees.framework.workflow.affinity.AffinityKindChangeHandler;
import com.ees.framework.workflow.affinity.DefaultAffinityKeyResolver;
import com.ees.framework.workflow.engine.BlockingWorkflowEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterAffinityKindMonitorTest {

    @Test
    void updatesWorkflowEngineAndRebindsOnKindChange() {
        InMemoryAssignmentService assignmentService = new InMemoryAssignmentService();
        DefaultAffinityKeyResolver resolver = new DefaultAffinityKeyResolver("equipmentId");
        BlockingWorkflowEngine engine = new BlockingWorkflowEngine(
            BlockingWorkflowEngine.BatchingOptions.defaults(),
            resolver
        );
        AtomicInteger rebinds = new AtomicInteger();
        AffinityKindChangeHandler handler = new AffinityKindChangeHandler(engine, rebinds::incrementAndGet);

        new ClusterAffinityKindMonitor(assignmentService, handler::onAffinityKindChanged);

        assignmentService.applyAssignments("group-1", List.of(
            new Assignment("group-1", 0, "node-1", Map.of("lotId", List.of("lot-1")), null, 1L, Instant.EPOCH)
        ));

        assertThat(resolver.defaultKind()).isEqualTo("lotId");
        assertThat(rebinds.get()).isEqualTo(1);
    }
}
