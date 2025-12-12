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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apache Ratis {@link RaftServer}를 구성/시작/중지하는 팩토리.
 * <p>
 * 설정에 정의된 그룹을 등록하고, 그룹별 {@link StateMachine}을 연결해 서버를 부트스트랩한다.
 */
public class RaftServerFactory {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(RaftServerFactory.class);

    private final RaftServerConfig config;
    private final Map<RaftGroupId, RaftGroup> groups = new ConcurrentHashMap<>();
    private final Map<RaftGroupId, StateMachine> stateMachines = new ConcurrentHashMap<>();
    // BaseStateMachine 동작을 수행한다.
    private final StateMachine defaultStateMachine = new BaseStateMachine();
    private volatile RaftServer server;
    // AtomicBoolean 동작을 수행한다.
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread shutdownHook;

    /**
     * 주어진 설정으로 팩토리를 생성한다.
     *
     * @param config Raft 서버 설정(널 불가)
     */
    public RaftServerFactory(RaftServerConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    /**
     * 설정에 정의된 그룹들을 등록한다.
     *
     * @param stateMachineFactory 그룹 ID별 상태머신 생성 함수(널 불가)
     */
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

    /**
     * 그룹과 상태머신을 등록한다.
     *
     * @param group Raft 그룹(널 불가)
     * @param stateMachine 상태머신(널 불가)
     */
    public void registerGroup(RaftGroup group, StateMachine stateMachine) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(stateMachine, "stateMachine must not be null");
        groups.put(group.getGroupId(), group);
        stateMachines.put(group.getGroupId(), stateMachine);
        if (server != null) {
            log.info("Registered group {} on running server (dynamic add pending)", group.getGroupId());
        }
    }

    /**
     * 서버를 시작한다.
     * <p>
     * 이미 시작된 경우 동일 인스턴스를 반환한다.
     *
     * @param peerId 로컬 peer ID(널 불가)
     * @return 시작된 RaftServer
     * @throws IOException 시작 실패 시
     */
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
        server.start();
        running.set(true);
        registerShutdownHook();
        return server;
    }

    /**
     * 서버를 중지한다.
     */
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
            running.set(false);
            removeShutdownHook();
        }
    }

    /**
     * 서버 실행 여부를 반환한다.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 간단한 헬스 정보를 반환한다.
     *
     * @return 헬스 요약
     */
    public RaftServerHealth health() {
        return new RaftServerHealth(running.get(), groups.size(), stateMachines.size());
    }
    // resolveStateMachine 동작을 수행한다.

    private StateMachine resolveStateMachine(RaftGroupId groupId) {
        // 등록된 상태머신이 없으면 기본 상태머신을 사용한다.
        return stateMachines.getOrDefault(groupId, defaultStateMachine);
    }
    // resolveGroupId 동작을 수행한다.

    private RaftGroupId resolveGroupId(String key, RaftGroupConfig groupConfig) {
        // 설정된 groupId(UUID)를 우선 사용하고, 유효하지 않으면 key 기반 deterministic UUID를 생성한다.
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
    // buildPeers 동작을 수행한다.

    private List<RaftPeer> buildPeers(Iterable<String> peerIds) {
        // peer ID 목록을 RaftPeer 목록으로 변환한다.
        List<RaftPeer> peers = new ArrayList<>();
        for (String peer : peerIds) {
            peers.add(toPeer(peer));
        }
        return peers;
    }
    // toPeer 동작을 수행한다.

    private RaftPeer toPeer(String id) {
        // 간단한 형태로 peer address를 구성한다(필요 시 향후 확장).
        RaftPeerId peerId = RaftPeerId.valueOf(id);
        return RaftPeer.newBuilder()
                .setId(peerId)
                .setAddress(id)
                .setClientAddress(id)
                .setAdminAddress(id)
                .build();
    }
    // registerShutdownHook 동작을 수행한다.

    private void registerShutdownHook() {
        // JVM 종료 시 서버를 정리하기 위한 shutdown hook을 등록한다.
        if (shutdownHook != null) {
            return;
        }
        shutdownHook = new Thread(this::stop, "raft-server-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
    // removeShutdownHook 동작을 수행한다.

    private void removeShutdownHook() {
        // shutdown hook을 제거한다(이미 종료 중이면 무시).
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // VM is shutting down.
            }
            shutdownHook = null;
        }
    }
}
