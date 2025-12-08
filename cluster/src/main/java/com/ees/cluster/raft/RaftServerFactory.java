package com.ees.cluster.raft;

import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.statemachine.StateMachine;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class RaftServerFactory {

    private static final Logger log = LoggerFactory.getLogger(RaftServerFactory.class);

    private final RaftServerConfig config;
    private final Map<RaftGroupId, RaftGroup> groups = new ConcurrentHashMap<>();
    private final Map<RaftGroupId, StateMachine> stateMachines = new ConcurrentHashMap<>();
    private final StateMachine defaultStateMachine = new BaseStateMachine();
    private volatile RaftServer server;

    public RaftServerFactory(RaftServerConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public void registerConfiguredGroups(Function<RaftGroupId, StateMachine> stateMachineFactory) {
        Objects.requireNonNull(stateMachineFactory, "stateMachineFactory must not be null");
        for (Map.Entry<String, RaftGroupConfig> entry : config.getGroups().entrySet()) {
            RaftGroupConfig groupConfig = entry.getValue() != null ? entry.getValue() : new RaftGroupConfig();
            RaftGroupId groupId = resolveGroupId(entry.getKey(), groupConfig);
            RaftGroup group = RaftGroup.valueOf(groupId, buildPeers(groupConfig.getPeers()));
            StateMachine machine = stateMachineFactory.apply(groupId);
            registerGroup(group, machine);
        }
    }

    public void registerGroup(RaftGroup group, StateMachine stateMachine) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(stateMachine, "stateMachine must not be null");
        groups.put(group.getGroupId(), group);
        stateMachines.put(group.getGroupId(), stateMachine);
        if (server != null) {
            log.info("Registered group {} on running server (dynamic add pending)", group.getGroupId());
        }
    }

    public synchronized RaftServer start(RaftPeerId peerId) throws IOException {
        Objects.requireNonNull(peerId, "peerId must not be null");
        if (server != null) {
            return server;
        }
        RaftProperties properties = new RaftProperties();
        RaftServerConfigKeys.setStorageDir(properties, List.of(Paths.get(config.getDataDir()).toFile()));
        // TODO: heartbeat/rpc timeout configuration once command handling is ready.
        RaftGroup bootstrap = groups.values().stream().findFirst().orElse(RaftGroup.emptyGroup());
        server = RaftServer.newBuilder()
                .setServerId(peerId)
                .setGroup(bootstrap)
                .setStateMachineRegistry(this::resolveStateMachine)
                .setProperties(properties)
                .build();
        return server;
    }

    public synchronized void stop() {
        if (server == null) {
            return;
        }
        try {
            server.close();
        } catch (IOException e) {
            log.warn("Failed to close RaftServer cleanly", e);
        } finally {
            server = null;
        }
    }

    private StateMachine resolveStateMachine(RaftGroupId groupId) {
        return stateMachines.getOrDefault(groupId, defaultStateMachine);
    }

    private RaftGroupId resolveGroupId(String key, RaftGroupConfig groupConfig) {
        if (groupConfig != null && groupConfig.getGroupId() != null && !groupConfig.getGroupId().isBlank()) {
            try {
                return RaftGroupId.valueOf(UUID.fromString(groupConfig.getGroupId()));
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid groupId {} for key {}, deriving deterministic id instead", groupConfig.getGroupId(), key);
            }
        }
        UUID derived = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        return RaftGroupId.valueOf(derived);
    }

    private List<RaftPeer> buildPeers(Iterable<String> peerIds) {
        List<RaftPeer> peers = new ArrayList<>();
        for (String peer : peerIds) {
            peers.add(toPeer(peer));
        }
        return peers;
    }

    private RaftPeer toPeer(String id) {
        RaftPeerId peerId = RaftPeerId.valueOf(id);
        return RaftPeer.newBuilder()
                .setId(peerId)
                .setAddress(id)
                .setClientAddress(id)
                .setAdminAddress(id)
                .build();
    }
}
