package com.ees.cluster.spring;

import com.ees.cluster.model.ClusterMode;
import com.ees.cluster.model.ClusterRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = "ees.cluster")
public class ClusterProperties {

    /**
        Cluster operating mode.
     */
    private ClusterMode mode = ClusterMode.KAFKA;

    /**
        Node id (should be unique per instance).
     */
    private String nodeId = "local-node";

    private String host = "localhost";
    private int port = 0;
    private Set<ClusterRole> roles = Set.of(ClusterRole.PIPELINE);
    private String zone = "default";
    private Duration heartbeatInterval = Duration.ofSeconds(5);
    private Duration heartbeatTimeout = Duration.ofSeconds(15);
    private Duration lockLease = Duration.ofSeconds(10);
    private Duration leaderLease = Duration.ofSeconds(10);
    private Duration assignmentTtl = Duration.ofMinutes(5);
    private String leaderGroup = "cluster";
    private boolean heartbeatEnabled = true;

    public ClusterMode getMode() {
        return mode;
    }

    public void setMode(ClusterMode mode) {
        this.mode = mode;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Set<ClusterRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<ClusterRole> roles) {
        this.roles = roles;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(Duration heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public Duration getLockLease() {
        return lockLease;
    }

    public void setLockLease(Duration lockLease) {
        this.lockLease = lockLease;
    }

    public Duration getLeaderLease() {
        return leaderLease;
    }

    public void setLeaderLease(Duration leaderLease) {
        this.leaderLease = leaderLease;
    }

    public Duration getAssignmentTtl() {
        return assignmentTtl;
    }

    public void setAssignmentTtl(Duration assignmentTtl) {
        this.assignmentTtl = assignmentTtl;
    }

    public String getLeaderGroup() {
        return leaderGroup;
    }

    public void setLeaderGroup(String leaderGroup) {
        this.leaderGroup = leaderGroup;
    }

    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }
}
