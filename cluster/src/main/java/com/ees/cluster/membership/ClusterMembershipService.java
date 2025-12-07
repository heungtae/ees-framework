package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.MembershipEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

public interface ClusterMembershipService {

    Mono<ClusterNodeRecord> join(ClusterNode node);

    Mono<ClusterNodeRecord> heartbeat(String nodeId);

    Mono<Void> leave(String nodeId);

    Mono<Void> remove(String nodeId);

    Mono<Optional<ClusterNodeRecord>> findNode(String nodeId);

    Mono<Map<String, ClusterNodeRecord>> view();

    Mono<Void> detectTimeouts();

    Flux<MembershipEvent> events();
}
