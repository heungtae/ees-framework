package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 로컬 노드의 join/heartbeat 전송과 타임아웃 감지를 스케줄링하는 헬퍼.
 */
public class HeartbeatMonitor implements AutoCloseable {

    private final ClusterMembershipService membershipService;
    private final ClusterMembershipProperties properties;
    private final ClusterNode localNode;
    private final ScheduledExecutorService scheduler;
    // AtomicBoolean 동작을 수행한다.
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> heartbeatLoop;
    private ScheduledFuture<?> detectionLoop;

    /**
     * 기본 스케줄러로 모니터를 생성한다.
     */
    public HeartbeatMonitor(ClusterMembershipService membershipService,
                            ClusterMembershipProperties properties,
                            ClusterNode localNode) {
        this(membershipService, properties, localNode, Executors.newScheduledThreadPool(2));
    }

    /**
     * 스케줄러를 주입해 모니터를 생성한다.
     */
    public HeartbeatMonitor(ClusterMembershipService membershipService,
                            ClusterMembershipProperties properties,
                            ClusterNode localNode,
                            ScheduledExecutorService scheduler) {
        this.membershipService = Objects.requireNonNull(membershipService, "membershipService must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.localNode = Objects.requireNonNull(localNode, "localNode must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    }

    /**
     * 모니터를 시작하고 로컬 노드를 join 처리한다.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        membershipService.join(localNode);
        heartbeatLoop = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    membershipService.heartbeat(localNode.nodeId());
                } catch (Exception ignored) {
                }
            },
            properties.heartbeatInterval().toMillis(),
            properties.heartbeatInterval().toMillis(),
            TimeUnit.MILLISECONDS
        );
        Duration detectionInterval = properties.heartbeatTimeout().dividedBy(2);
        detectionLoop = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    membershipService.detectTimeouts();
                } catch (Exception ignored) {
                }
            },
            detectionInterval.toMillis(),
            detectionInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }
    /**
     * close를 수행한다.
     */

    @Override
    public void close() {
        stop();
    }

    /**
     * 모니터를 중지하고 로컬 노드를 leave 처리한다.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (heartbeatLoop != null) {
            heartbeatLoop.cancel(true);
        }
        if (detectionLoop != null) {
            detectionLoop.cancel(true);
        }
        membershipService.leave(localNode.nodeId());
    }
}
