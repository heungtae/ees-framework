package com.ees.cluster.raft;

import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeerId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Wraps a Ratis {@link RaftClient} to expose leader information through the cluster API.
 * Ratis decides leadership; this bridge only reads it and emits changes.
 */
public class RatisLeaderElectionBridge implements LeaderElectionService {

    private final RaftClient raftClient;
    private final RaftGroupId groupId;
    private final Clock clock;
    private final CopyOnWriteArrayList<Consumer<LeaderInfo>> listeners = new CopyOnWriteArrayList<>();
    private volatile LeaderInfo lastKnown;
    /**
     * 인스턴스를 생성한다.
     * @param raftClient 
     * @param groupId 
     */

    public RatisLeaderElectionBridge(RaftClient raftClient, RaftGroupId groupId) {
        this(raftClient, groupId, Clock.systemUTC());
    }
    /**
     * 인스턴스를 생성한다.
     * @param raftClient 
     * @param groupId 
     * @param clock 
     */

    public RatisLeaderElectionBridge(RaftClient raftClient, RaftGroupId groupId, Clock clock) {
        this.raftClient = Objects.requireNonNull(raftClient, "raftClient must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }
    /**
     * tryAcquireLeader를 수행한다.
     * @param groupId 
     * @param nodeId 
     * @param mode 
     * @param leaseDuration 
     * @return 
     */

    @Override
    public Optional<LeaderInfo> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration) {
        // Ratis handles elections; simply fetch the leader.
        return getLeader(groupId);
    }
    /**
     * release를 수행한다.
     * @param groupId 
     * @param nodeId 
     * @return 
     */

    @Override
    public boolean release(String groupId, String nodeId) {
        // Ratis handles step-down; instructing from here isn't supported.
        return false;
    }
    /**
     * leader를 반환한다.
     * @param group 
     * @return 
     */

    @Override
    public Optional<LeaderInfo> getLeader(String group) {
        if (!this.groupId.getUuid().toString().equals(group)) {
            return Optional.empty();
        }
        RaftPeerId peerId = raftClient.getLeaderId();
        if (peerId == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        LeaderInfo info = new LeaderInfo(group, peerId.toString(), LeaderElectionMode.RAFT, 0L, now, now.plusSeconds(5));
        lastKnown = info;
        emit(info);
        return Optional.of(info);
    }
    /**
     * watch를 수행한다.
     * @param group 
     * @param consumer 
     */

    @Override
    public void watch(String group, Consumer<LeaderInfo> consumer) {
        if (this.groupId.getUuid().toString().equals(group) && lastKnown != null) {
            consumer.accept(lastKnown);
        }
        listeners.add(info -> {
            if (info.groupId().equals(group)) {
                consumer.accept(info);
            }
        });
    }
    // emit 동작을 수행한다.

    private void emit(LeaderInfo info) {
        for (Consumer<LeaderInfo> listener : listeners) {
            listener.accept(info);
        }
    }
}
