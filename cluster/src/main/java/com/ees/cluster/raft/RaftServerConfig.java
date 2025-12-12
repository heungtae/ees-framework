package com.ees.cluster.raft;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Raft 서버 동작을 제어하는 설정 값 모음.
 */
public class RaftServerConfig {

    private String dataDir = "data/raft";
    private int peerCount = 3;
    // ofMillis 동작을 수행한다.
    private Duration heartbeatInterval = Duration.ofMillis(500);
    // ofSeconds 동작을 수행한다.
    private Duration rpcTimeout = Duration.ofSeconds(5);
    private long snapshotThreshold = 1_000L;
    private long snapshotSizeThresholdBytes = 64 * 1024 * 1024;
    private SnapshotStore snapshotStore = SnapshotStore.FILE;
    private Map<String, RaftGroupConfig> groups = new HashMap<>();

    /**
     * 스냅샷 저장소 종류.
     */
    public enum SnapshotStore {
        FILE,
        DB,
        KAFKA_KTABLE
    }

    /**
     * Raft 데이터 디렉터리를 반환한다.
     */
    public String getDataDir() {
        return dataDir;
    }

    /**
     * Raft 데이터 디렉터리를 설정한다.
     *
     * @param dataDir 디렉터리 경로(널 불가)
     */
    public void setDataDir(String dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir must not be null");
    }

    /**
     * peer 수(3 또는 5)를 반환한다.
     */
    public int getPeerCount() {
        return peerCount;
    }

    /**
     * peer 수를 설정한다(3 또는 5만 허용).
     *
     * @param peerCount peer 수
     * @throws IllegalArgumentException 3/5가 아닌 경우
     */
    public void setPeerCount(int peerCount) {
        if (peerCount != 3 && peerCount != 5) {
            throw new IllegalArgumentException("peerCount must be 3 or 5");
        }
        this.peerCount = peerCount;
    }

    /**
     * 하트비트 주기를 반환한다.
     */
    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * 하트비트 주기를 설정한다.
     */
    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
    }

    /**
     * RPC 타임아웃을 반환한다.
     */
    public Duration getRpcTimeout() {
        return rpcTimeout;
    }

    /**
     * RPC 타임아웃을 설정한다.
     */
    public void setRpcTimeout(Duration rpcTimeout) {
        this.rpcTimeout = Objects.requireNonNull(rpcTimeout, "rpcTimeout must not be null");
    }

    /**
     * 스냅샷 생성 임계치(커밋/적용 횟수 기반)를 반환한다.
     */
    public long getSnapshotThreshold() {
        return snapshotThreshold;
    }

    /**
     * 스냅샷 생성 임계치를 설정한다.
     *
     * @throws IllegalArgumentException 음수인 경우
     */
    public void setSnapshotThreshold(long snapshotThreshold) {
        if (snapshotThreshold < 0) {
            throw new IllegalArgumentException("snapshotThreshold must be >= 0");
        }
        this.snapshotThreshold = snapshotThreshold;
    }

    /**
     * 스냅샷 크기 임계치(바이트)를 반환한다.
     */
    public long getSnapshotSizeThresholdBytes() {
        return snapshotSizeThresholdBytes;
    }

    /**
     * 스냅샷 크기 임계치(바이트)를 설정한다.
     *
     * @throws IllegalArgumentException 음수인 경우
     */
    public void setSnapshotSizeThresholdBytes(long snapshotSizeThresholdBytes) {
        if (snapshotSizeThresholdBytes < 0) {
            throw new IllegalArgumentException("snapshotSizeThresholdBytes must be >= 0");
        }
        this.snapshotSizeThresholdBytes = snapshotSizeThresholdBytes;
    }

    /**
     * 스냅샷 저장소 종류를 반환한다.
     */
    public SnapshotStore getSnapshotStore() {
        return snapshotStore;
    }

    /**
     * 스냅샷 저장소 종류를 설정한다.
     */
    public void setSnapshotStore(SnapshotStore snapshotStore) {
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore must not be null");
    }

    /**
     * 구성된 그룹 맵을 반환한다.
     */
    public Map<String, RaftGroupConfig> getGroups() {
        return groups;
    }

    /**
     * 구성할 그룹 맵을 설정한다.
     *
     * @param groups 그룹 맵(널이면 빈 맵)
     */
    public void setGroups(Map<String, RaftGroupConfig> groups) {
        this.groups = groups == null ? new HashMap<>() : new HashMap<>(groups);
    }
}
