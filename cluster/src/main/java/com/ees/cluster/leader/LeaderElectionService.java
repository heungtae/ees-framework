package com.ees.cluster.leader;

import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

public interface LeaderElectionService {

    Mono<Optional<LeaderInfo>> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration);

    Mono<Boolean> release(String groupId, String nodeId);

    Mono<Optional<LeaderInfo>> getLeader(String groupId);

    Flux<LeaderInfo> watch(String groupId);
}
