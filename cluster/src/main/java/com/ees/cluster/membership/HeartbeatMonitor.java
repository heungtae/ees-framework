package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class HeartbeatMonitor implements AutoCloseable {

    private final ClusterMembershipService membershipService;
    private final ClusterMembershipProperties properties;
    private final ClusterNode localNode;
    private final Scheduler scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Disposable heartbeatLoop;
    private Disposable detectionLoop;

    public HeartbeatMonitor(ClusterMembershipService membershipService,
                            ClusterMembershipProperties properties,
                            ClusterNode localNode) {
        this(membershipService, properties, localNode, Schedulers.parallel());
    }

    public HeartbeatMonitor(ClusterMembershipService membershipService,
                            ClusterMembershipProperties properties,
                            ClusterNode localNode,
                            Scheduler scheduler) {
        this.membershipService = Objects.requireNonNull(membershipService, "membershipService must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.localNode = Objects.requireNonNull(localNode, "localNode must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        membershipService.join(localNode).block();
        heartbeatLoop = Flux.interval(properties.heartbeatInterval(), scheduler)
                .flatMap(tick -> membershipService.heartbeat(localNode.nodeId()))
                .subscribe();
        Duration detectionInterval = properties.heartbeatTimeout().dividedBy(2);
        detectionLoop = Flux.interval(detectionInterval, scheduler)
                .flatMap(tick -> membershipService.detectTimeouts())
                .subscribe();
    }

    @Override
    public void close() {
        stop();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (heartbeatLoop != null) {
            heartbeatLoop.dispose();
        }
        if (detectionLoop != null) {
            detectionLoop.dispose();
        }
        membershipService.leave(localNode.nodeId()).block();
    }
}
