package com.ees.framework.workflow;

import com.ees.framework.workflow.engine.WorkflowEngine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 워크플로 엔진 실행 옵션을 프로퍼티로 바인딩하는 설정 클래스.
 * <p>
 * {@code ees.workflow.*} 프리픽스로 바인딩되며, 내부적으로 {@link WorkflowEngine.BatchingOptions}로 변환된다.
 */
@ConfigurationProperties(prefix = "ees.workflow")
@Validated
public class WorkflowProperties {

    private int queueCapacity = 256;
    private int batchSize = 32;
    // ofMillis 동작을 수행한다.
    private Duration batchTimeout = Duration.ofMillis(200);
    // ofSeconds 동작을 수행한다.
    private Duration cleanupIdleAfter = Duration.ofSeconds(30);
    private WorkflowEngine.BackpressurePolicy backpressurePolicy = WorkflowEngine.BackpressurePolicy.BLOCK;
    private boolean continuous = false;

    /**
     * 현재 프로퍼티 값을 {@link WorkflowEngine.BatchingOptions}로 변환한다.
     *
     * @return 배치 옵션
     * @throws IllegalArgumentException queueCapacity/batchSize가 0 이하인 경우
     * @throws IllegalStateException 필수 Duration 설정이 누락된 경우
     */
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

    /**
     * per-key 큐 용량을 반환한다.
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * per-key 큐 용량을 설정한다.
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * 배치 크기를 반환한다.
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 배치 크기를 설정한다.
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 배치 타임아웃을 반환한다.
     */
    public Duration getBatchTimeout() {
        return batchTimeout;
    }

    /**
     * 배치 타임아웃을 설정한다.
     */
    public void setBatchTimeout(Duration batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    /**
     * idle 워커 정리 시간을 반환한다.
     */
    public Duration getCleanupIdleAfter() {
        return cleanupIdleAfter;
    }

    /**
     * idle 워커 정리 시간을 설정한다.
     */
    public void setCleanupIdleAfter(Duration cleanupIdleAfter) {
        this.cleanupIdleAfter = cleanupIdleAfter;
    }

    /**
     * 백프레셔 정책을 반환한다.
     */
    public WorkflowEngine.BackpressurePolicy getBackpressurePolicy() {
        return backpressurePolicy;
    }

    /**
     * 백프레셔 정책을 설정한다.
     */
    public void setBackpressurePolicy(WorkflowEngine.BackpressurePolicy backpressurePolicy) {
        this.backpressurePolicy = backpressurePolicy;
    }

    /**
     * continuous 모드 여부를 반환한다.
     */
    public boolean isContinuous() {
        return continuous;
    }

    /**
     * continuous 모드 여부를 설정한다.
     */
    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }
}
