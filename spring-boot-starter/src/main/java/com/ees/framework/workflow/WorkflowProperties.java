package com.ees.framework.workflow;

import com.ees.framework.workflow.engine.WorkflowEngine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "ees.workflow")
@Validated
public class WorkflowProperties {

    private int queueCapacity = 256;
    private int batchSize = 32;
    private Duration batchTimeout = Duration.ofMillis(200);
    private Duration cleanupIdleAfter = Duration.ofSeconds(30);
    private WorkflowEngine.BackpressurePolicy backpressurePolicy = WorkflowEngine.BackpressurePolicy.BLOCK;
    private boolean continuous = false;

    public WorkflowEngine.BatchingOptions toBatchingOptions() {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be > 0");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
        if (batchTimeout == null || cleanupIdleAfter == null) {
            throw new IllegalStateException("batchTimeout and cleanupIdleAfter must be configured");
        }
        return new WorkflowEngine.BatchingOptions(
            queueCapacity,
            batchSize,
            batchTimeout,
            cleanupIdleAfter,
            backpressurePolicy,
            continuous
        );
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getBatchTimeout() {
        return batchTimeout;
    }

    public void setBatchTimeout(Duration batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    public Duration getCleanupIdleAfter() {
        return cleanupIdleAfter;
    }

    public void setCleanupIdleAfter(Duration cleanupIdleAfter) {
        this.cleanupIdleAfter = cleanupIdleAfter;
    }

    public WorkflowEngine.BackpressurePolicy getBackpressurePolicy() {
        return backpressurePolicy;
    }

    public void setBackpressurePolicy(WorkflowEngine.BackpressurePolicy backpressurePolicy) {
        this.backpressurePolicy = backpressurePolicy;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }
}
