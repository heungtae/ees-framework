package com.ees.cluster.leader;

import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

public interface LeaderElectionService {

    Optional<LeaderInfo> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration);

    boolean release(String groupId, String nodeId);

    Optional<LeaderInfo> getLeader(String groupId);

    void watch(String groupId, Consumer<LeaderInfo> consumer);
}
