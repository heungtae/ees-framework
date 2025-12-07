package com.ees.cluster.raft;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeerId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps a Ratis {@link RaftClient} to expose leader information through the cluster API.
 * Ratis decides leadership; this bridge only reads it and emits changes.
 */
public class RatisLeaderElectionBridge implements LeaderElectionService {

    private final RaftClient raftClient;
    private final RaftGroupId groupId;
    private final Clock clock;
    private final Sinks.Many<LeaderInfo> sink = Sinks.many().multicast().onBackpressureBuffer();
    private volatile LeaderInfo lastKnown;

    public RatisLeaderElectionBridge(RaftClient raftClient, RaftGroupId groupId) {
        this(raftClient, groupId, Clock.systemUTC());
    }

    public RatisLeaderElectionBridge(RaftClient raftClient, RaftGroupId groupId, Clock clock) {
        this.raftClient = Objects.requireNonNull(raftClient, "raftClient must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<Optional<LeaderInfo>> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration) {
        // Ratis handles elections; simply fetch the leader.
        return getLeader(groupId);
    }

    @Override
    public Mono<Boolean> release(String groupId, String nodeId) {
        // Ratis handles step-down; instructing from here isn't supported.
        return Mono.just(false);
    }

    @Override
    public Mono<Optional<LeaderInfo>> getLeader(String group) {
        if (!this.groupId.getUuid().toString().equals(group)) {
            return Mono.just(Optional.empty());
        }
        return Mono.fromCallable(() -> {
            RaftPeerId peerId = raftClient.getLeaderId();
            if (peerId == null) {
                return Optional.<LeaderInfo>empty();
            }
            Instant now = clock.instant();
            LeaderInfo info = new LeaderInfo(group, peerId.toString(), LeaderElectionMode.RAFT, 0L, now, now.plusSeconds(5));
            lastKnown = info;
            sink.emitNext(info, Sinks.EmitFailureHandler.FAIL_FAST);
            return Optional.of(info);
        });
    }

    @Override
    public Flux<LeaderInfo> watch(String group) {
        if (this.groupId.getUuid().toString().equals(group) && lastKnown != null) {
            sink.emitNext(lastKnown, Sinks.EmitFailureHandler.FAIL_FAST);
        }
        return sink.asFlux().filter(info -> info.groupId().equals(group));
    }
}
