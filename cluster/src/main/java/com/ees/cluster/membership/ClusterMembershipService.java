package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.MembershipEvent;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface ClusterMembershipService {

    ClusterNodeRecord join(ClusterNode node);

    ClusterNodeRecord heartbeat(String nodeId);

    void leave(String nodeId);

    void remove(String nodeId);

    Optional<ClusterNodeRecord> findNode(String nodeId);

    Map<String, ClusterNodeRecord> view();

    void detectTimeouts();

    void events(Consumer<MembershipEvent> consumer);
}
