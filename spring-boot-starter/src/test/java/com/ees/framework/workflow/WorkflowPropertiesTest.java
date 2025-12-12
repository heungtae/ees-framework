package com.ees.framework.workflow;

import com.ees.framework.workflow.engine.BlockingWorkflowEngine;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowPropertiesTest {

    @Test
    void mapsToBatchingOptions() {
        WorkflowProperties props = new WorkflowProperties();
        props.setQueueCapacity(10);
        props.setBatchSize(5);
        props.setBatchTimeout(Duration.ofMillis(50));
        props.setCleanupIdleAfter(Duration.ofSeconds(2));
        props.setBackpressurePolicy(BlockingWorkflowEngine.BackpressurePolicy.ERROR);
        props.setContinuous(true);

        BlockingWorkflowEngine.BatchingOptions options = props.toBatchingOptions();

        assertThat(options.queueCapacity()).isEqualTo(10);
        assertThat(options.batchSize()).isEqualTo(5);
        assertThat(options.batchTimeout()).isEqualTo(Duration.ofMillis(50));
        assertThat(options.cleanupIdleAfter()).isEqualTo(Duration.ofSeconds(2));
        assertThat(options.backpressurePolicy()).isEqualTo(BlockingWorkflowEngine.BackpressurePolicy.ERROR);
        assertThat(options.continuous()).isTrue();
    }

    @Test
    void failsWhenTimeoutsMissing() {
        WorkflowProperties props = new WorkflowProperties();
        props.setBatchTimeout(null);
        assertThrows(IllegalStateException.class, props::toBatchingOptions);
    }

    @Test
    void failsWhenCapacityInvalid() {
        WorkflowProperties props = new WorkflowProperties();
        props.setQueueCapacity(0);
        assertThrows(IllegalArgumentException.class, props::toBatchingOptions);
    }
}
