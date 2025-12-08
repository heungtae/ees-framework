package com.ees.cluster.raft;

import com.ees.cluster.lock.DefaultDistributedLockService;
import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.model.Assignment;
import com.ees.cluster.raft.command.AssignPartitionCommand;
import com.ees.cluster.raft.command.CommandType;
import com.ees.cluster.raft.command.LockCommand;
import com.ees.cluster.raft.command.RaftCommandCodec;
import com.ees.cluster.raft.command.RaftCommandEnvelope;
import com.ees.cluster.raft.snapshot.FileClusterSnapshotStore;
import com.ees.cluster.state.ClusterStateRepository;
import com.ees.cluster.state.InMemoryClusterStateRepository;
import org.apache.ratis.RaftConfigKeys;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.rpc.SupportedRpcType;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedRatisClusterTest {

    @TempDir
    Path tempDir;

    @Test
    void replicatesAssignmentsAndLocksAcrossPeers() throws Exception {
        RaftGroupId groupId = RaftGroupId.valueOf(UUID.randomUUID());
        String groupIdString = groupId.getUuid().toString();
        List<PeerEndpoint> endpoints = List.of(
                new PeerEndpoint("n1", freePort()),
                new PeerEndpoint("n2", freePort()),
                new PeerEndpoint("n3", freePort())
        );
        List<RaftPeer> peers = endpoints.stream()
                .map(endpoint -> RaftPeer.newBuilder()
                        .setId(endpoint.peerId)
                        .setAddress(endpoint.address)
                        .setClientAddress(endpoint.address)
                        .setAdminAddress(endpoint.address)
                        .build())
                .toList();
        RaftGroup group = RaftGroup.valueOf(groupId, peers);

        Map<RaftPeerId, RaftAssignmentService> assignmentsByPeer = new HashMap<>();
        Map<RaftPeerId, DistributedLockService> locksByPeer = new HashMap<>();
        List<RaftServer> servers = new ArrayList<>();

        for (PeerEndpoint endpoint : endpoints) {
            Path nodeDir = Files.createDirectories(tempDir.resolve(endpoint.peerId.toString()));
            ClusterStateRepository repository = new InMemoryClusterStateRepository();
            RaftAssignmentService assignmentService = new RaftAssignmentService(repository, Duration.ofMinutes(5));
            DistributedLockService lockService = new DefaultDistributedLockService(repository);
            ClusterStateMachine machine = new ClusterStateMachine(
                    assignmentService,
                    lockService,
                    new FileClusterSnapshotStore(nodeDir.resolve("snapshots")));

            RaftProperties properties = new RaftProperties();
            RaftConfigKeys.Rpc.setType(properties, SupportedRpcType.NETTY);
            RaftServerConfigKeys.setStorageDir(properties, List.of(nodeDir.toFile()));

            RaftServer server = RaftServer.newBuilder()
                    .setServerId(endpoint.peerId)
                    .setGroup(group)
                    .setStateMachineRegistry(gid -> machine)
                    .setProperties(properties)
                    .build();
            server.start();
            servers.add(server);
            assignmentsByPeer.put(endpoint.peerId, assignmentService);
            locksByPeer.put(endpoint.peerId, lockService);
        }

        RaftProperties clientProps = new RaftProperties();
        RaftConfigKeys.Rpc.setType(clientProps, SupportedRpcType.NETTY);

        try (RaftClient client = RaftClient.newBuilder()
                .setProperties(clientProps)
                .setRaftGroup(group)
                .build()) {

            AssignPartitionCommand assign = new AssignPartitionCommand(groupIdString, 0, "n1",
                    List.of("eq-1", "eq-2"), null);
            RaftCommandEnvelope assignmentEnvelope = new RaftCommandEnvelope(1L, CommandType.ASSIGN_PARTITION, assign);
            client.io().send(Message.valueOf(org.apache.ratis.thirdparty.com.google.protobuf.ByteString.copyFrom(
                    RaftCommandCodec.serialize(assignmentEnvelope))));

            LockCommand lock = new LockCommand("rebalance", "n1", 10_000L, Map.of());
            RaftCommandEnvelope lockEnvelope = new RaftCommandEnvelope(1L, CommandType.LOCK_ACQUIRE, lock);
            client.io().send(Message.valueOf(org.apache.ratis.thirdparty.com.google.protobuf.ByteString.copyFrom(
                    RaftCommandCodec.serialize(lockEnvelope))));

            awaitCondition(() -> assignmentsByPeer.values().stream()
                    .allMatch(service -> service.findAssignment(groupIdString, 0)
                            .blockOptional().orElse(Optional.empty()).isPresent()), 10_000);

            awaitCondition(() -> locksByPeer.values().stream()
                    .allMatch(service -> service.getLock("rebalance").blockOptional().orElse(Optional.empty()).isPresent()), 10_000);

            for (RaftAssignmentService service : assignmentsByPeer.values()) {
                Optional<Assignment> assignment = service.findAssignment(groupIdString, 0).block();
                assertTrue(assignment.isPresent());
                assertTrue(assignment.get().equipmentIds().contains("eq-1"));
            }
        } finally {
            for (RaftServer server : servers) {
                try {
                    server.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void awaitCondition(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        throw new AssertionError("Condition not satisfied within timeout");
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            Assumptions.assumeTrue(false, "Local sockets are not permitted in this environment: " + ex.getMessage());
            throw ex;
        }
    }

    private record PeerEndpoint(RaftPeerId peerId, String address) {
        PeerEndpoint(String id, int port) {
            this(RaftPeerId.valueOf(id), "127.0.0.1:" + port);
        }
    }
}
