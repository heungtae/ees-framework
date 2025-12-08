package com.ees.cluster.raft;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RaftServerConfig {

    private String dataDir = "data/raft";
    private int peerCount = 3;
    private Duration heartbeatInterval = Duration.ofMillis(500);
    private Duration rpcTimeout = Duration.ofSeconds(5);
    private long snapshotThreshold = 1_000L;
    private SnapshotStore snapshotStore = SnapshotStore.FILE;
    private Map<String, RaftGroupConfig> groups = new HashMap<>();

    public enum SnapshotStore {
        FILE,
        DB,
        KAFKA_KTABLE
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir must not be null");
    }

    public int getPeerCount() {
        return peerCount;
    }

    public void setPeerCount(int peerCount) {
        if (peerCount != 3 && peerCount != 5) {
            throw new IllegalArgumentException("peerCount must be 3 or 5");
        }
        this.peerCount = peerCount;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
    }

    public Duration getRpcTimeout() {
        return rpcTimeout;
    }

    public void setRpcTimeout(Duration rpcTimeout) {
        this.rpcTimeout = Objects.requireNonNull(rpcTimeout, "rpcTimeout must not be null");
    }

    public long getSnapshotThreshold() {
        return snapshotThreshold;
    }

    public void setSnapshotThreshold(long snapshotThreshold) {
        if (snapshotThreshold < 0) {
            throw new IllegalArgumentException("snapshotThreshold must be >= 0");
        }
        this.snapshotThreshold = snapshotThreshold;
    }

    public SnapshotStore getSnapshotStore() {
        return snapshotStore;
    }

    public void setSnapshotStore(SnapshotStore snapshotStore) {
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore must not be null");
    }

    public Map<String, RaftGroupConfig> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, RaftGroupConfig> groups) {
        this.groups = groups == null ? new HashMap<>() : new HashMap<>(groups);
    }
}
