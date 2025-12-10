package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.ClusterNodeStatus;
import com.ees.cluster.model.ClusterRole;
import com.ees.cluster.model.MembershipEvent;
import com.ees.cluster.model.MembershipEventType;
import com.ees.cluster.state.InMemoryClusterStateRepository;
import com.ees.cluster.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultClusterMembershipServiceTest {

    private MutableClock clock;
    private InMemoryClusterStateRepository repository;
    private DefaultClusterMembershipService membershipService;
    private ClusterMembershipProperties properties;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        repository = new InMemoryClusterStateRepository(clock);
        properties = new ClusterMembershipProperties(Duration.ofSeconds(1), Duration.ofSeconds(5));
        membershipService = new DefaultClusterMembershipService(repository, properties, clock);
    }

    @Test
    void joinAndHeartbeatEmitEvents() {
        ClusterNode node = node("node-1");
        java.util.List<MembershipEventType> events = new java.util.ArrayList<>();
        membershipService.events(event -> events.add(event.type()));

        membershipService.join(node);
        clock.advance(Duration.ofSeconds(1));
        membershipService.heartbeat(node.nodeId());

        assertEquals(java.util.List.of(MembershipEventType.JOINED, MembershipEventType.HEARTBEAT), events);
    }

    @Test
    void detectTimeoutsMovesNodeToSuspectThenDown() {
        ClusterNode node = node("node-1");
        java.util.List<MembershipEventType> events = new java.util.ArrayList<>();
        membershipService.events(event -> events.add(event.type()));

        membershipService.join(node);
        clock.advance(Duration.ofSeconds(6));
        membershipService.detectTimeouts();
        Optional<ClusterNodeRecord> suspect = membershipService.findNode(node.nodeId());
        assertTrue(suspect.isPresent());
        assertEquals(ClusterNodeStatus.SUSPECT, suspect.get().status());

        clock.advance(Duration.ofSeconds(4));
        membershipService.detectTimeouts();
        Optional<ClusterNodeRecord> down = membershipService.findNode(node.nodeId());
        assertTrue(down.isPresent());
        assertEquals(ClusterNodeStatus.DOWN, down.get().status());

        assertEquals(java.util.List.of(MembershipEventType.JOINED, MembershipEventType.SUSPECTED, MembershipEventType.DOWN), events);
    }

    private ClusterNode node(String id) {
        return new ClusterNode(id, "localhost", 9092, Set.of(ClusterRole.SOURCE), "zone-a", Map.of(), "1.0.0");
    }
}
