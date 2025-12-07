package com.ees.cluster.kafka;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified leader view for Kafka mode. Kafka's partition assignments determine
 * active nodes; this class exposes a group-level leader with lease semantics
 * to be used for non-Kafka coordinated tasks.
 */
public class KafkaLeaderElectionService implements LeaderElectionService {

    private final ConcurrentHashMap<String, LeaderInfo> leaders = new ConcurrentHashMap<>();
    private final Sinks.Many<LeaderInfo> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Clock clock;

    public KafkaLeaderElectionService() {
        this(Clock.systemUTC());
    }

    public KafkaLeaderElectionService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<Optional<LeaderInfo>> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(leaseDuration, "leaseDuration must not be null");
        Instant now = clock.instant();
        LeaderInfo current = leaders.get(groupId);
        long term = current == null ? 1L : current.term() + 1;
        LeaderInfo updated = new LeaderInfo(groupId, nodeId, mode, term, now, now.plus(leaseDuration));
        leaders.put(groupId, updated);
        emit(updated);
        return Mono.just(Optional.of(updated));
    }

    @Override
    public Mono<Boolean> release(String groupId, String nodeId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        LeaderInfo current = leaders.get(groupId);
        if (current != null && current.leaderNodeId().equals(nodeId)) {
            leaders.remove(groupId);
            return Mono.just(true);
        }
        return Mono.just(false);
    }

    @Override
    public Mono<Optional<LeaderInfo>> getLeader(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        LeaderInfo current = leaders.get(groupId);
        if (current == null) {
            return Mono.just(Optional.empty());
        }
        if (isExpired(current)) {
            leaders.remove(groupId);
            return Mono.just(Optional.empty());
        }
        return Mono.just(Optional.of(current));
    }

    @Override
    public Flux<LeaderInfo> watch(String groupId) {
        Objects.requireNonNull(groupId, "groupId must not be null");
        return sink.asFlux().filter(info -> info.groupId().equals(groupId));
    }

    private boolean isExpired(LeaderInfo info) {
        Instant now = clock.instant();
        return !info.leaseExpiresAt().isAfter(now);
    }

    private void emit(LeaderInfo info) {
        sink.emitNext(info, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
