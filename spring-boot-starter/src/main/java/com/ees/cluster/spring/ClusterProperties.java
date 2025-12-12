package com.ees.cluster.spring;

import com.ees.cluster.model.AffinityKeys;
import com.ees.cluster.model.ClusterMode;
import com.ees.cluster.model.ClusterRole;
import com.ees.cluster.raft.RaftServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

/**
 * 클러스터 관련 설정 프로퍼티.
 * <p>
 * {@code ees.cluster.*} 프리픽스로 바인딩된다.
 */
@ConfigurationProperties(prefix = "ees.cluster")
public class ClusterProperties {

    /**
     * 클러스터 운영 모드.
     */
    private ClusterMode mode = ClusterMode.KAFKA;

    /**
     * 노드 ID(인스턴스별 유니크 권장).
     */
    private String nodeId = "local-node";

    private String host = "localhost";
    private int port = 19090;
    // of 동작을 수행한다.
    private Set<ClusterRole> roles = Set.of(ClusterRole.PIPELINE);
    private String zone = "default";
    // ofSeconds 동작을 수행한다.
    private Duration heartbeatInterval = Duration.ofSeconds(5);
    // ofSeconds 동작을 수행한다.
    private Duration heartbeatTimeout = Duration.ofSeconds(15);
    // ofSeconds 동작을 수행한다.
    private Duration lockLease = Duration.ofSeconds(10);
    // ofSeconds 동작을 수행한다.
    private Duration leaderLease = Duration.ofSeconds(10);
    // ofMinutes 동작을 수행한다.
    private Duration assignmentTtl = Duration.ofMinutes(5);
    private String leaderGroup = "cluster";
    private boolean heartbeatEnabled = true;
    // RaftServerConfig 동작을 수행한다.
    private RaftServerConfig raft = new RaftServerConfig();
    /**
     * 키 할당 시 기본 affinity kind (예: equipmentId, lotId).
     */
    private String assignmentAffinityKind = AffinityKeys.DEFAULT;

    /**
     * 클러스터 운영 모드를 반환한다.
     */
    public ClusterMode getMode() {
        return mode;
    }

    /**
     * 클러스터 운영 모드를 설정한다.
     */
    public void setMode(ClusterMode mode) {
        this.mode = mode;
    }

    /**
     * 노드 ID를 반환한다.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 노드 ID를 설정한다.
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 호스트를 반환한다.
     */
    public String getHost() {
        return host;
    }

    /**
     * 호스트를 설정한다.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 포트를 반환한다.
     */
    public int getPort() {
        return port;
    }

    /**
     * 포트를 설정한다.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 노드 역할 목록을 반환한다.
     */
    public Set<ClusterRole> getRoles() {
        return roles;
    }

    /**
     * 노드 역할 목록을 설정한다.
     */
    public void setRoles(Set<ClusterRole> roles) {
        this.roles = roles;
    }

    /**
     * 존/가용영역을 반환한다.
     */
    public String getZone() {
        return zone;
    }

    /**
     * 존/가용영역을 설정한다.
     */
    public void setZone(String zone) {
        this.zone = zone;
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
        this.heartbeatInterval = heartbeatInterval;
    }

    /**
     * 하트비트 타임아웃을 반환한다.
     */
    public Duration getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    /**
     * 하트비트 타임아웃을 설정한다.
     */
    public void setHeartbeatTimeout(Duration heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    /**
     * 락 리스(lease) 기본값을 반환한다.
     */
    public Duration getLockLease() {
        return lockLease;
    }

    /**
     * 락 리스(lease) 기본값을 설정한다.
     */
    public void setLockLease(Duration lockLease) {
        this.lockLease = lockLease;
    }

    /**
     * 리더 리스(lease) 기본값을 반환한다.
     */
    public Duration getLeaderLease() {
        return leaderLease;
    }

    /**
     * 리더 리스(lease) 기본값을 설정한다.
     */
    public void setLeaderLease(Duration leaderLease) {
        this.leaderLease = leaderLease;
    }

    /**
     * 할당 정보 TTL을 반환한다.
     */
    public Duration getAssignmentTtl() {
        return assignmentTtl;
    }

    /**
     * 할당 정보 TTL을 설정한다.
     */
    public void setAssignmentTtl(Duration assignmentTtl) {
        this.assignmentTtl = assignmentTtl;
    }

    /**
     * 리더 그룹 ID를 반환한다.
     */
    public String getLeaderGroup() {
        return leaderGroup;
    }

    /**
     * 리더 그룹 ID를 설정한다.
     */
    public void setLeaderGroup(String leaderGroup) {
        this.leaderGroup = leaderGroup;
    }

    /**
     * 하트비트 기능 활성화 여부를 반환한다.
     */
    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    /**
     * 하트비트 기능 활성화 여부를 설정한다.
     */
    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }

    /**
     * Raft 서버 설정을 반환한다.
     */
    public RaftServerConfig getRaft() {
        return raft;
    }

    /**
     * Raft 서버 설정을 설정한다.
     */
    public void setRaft(RaftServerConfig raft) {
        this.raft = raft;
    }

    /**
     * 키 할당 기본 affinity kind를 반환한다.
     */
    public String getAssignmentAffinityKind() {
        return assignmentAffinityKind;
    }

    /**
     * 키 할당 기본 affinity kind를 설정한다.
     */
    public void setAssignmentAffinityKind(String assignmentAffinityKind) {
        this.assignmentAffinityKind = assignmentAffinityKind;
    }
}
