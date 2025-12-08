package com.ees.cluster.raft;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RaftGroupConfig {

    private String groupId;
    private Set<String> peers = new HashSet<>();

    public RaftGroupConfig() {
    }

    public RaftGroupConfig(String groupId, Set<String> peers) {
        this.groupId = groupId;
        setPeers(peers);
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Set<String> getPeers() {
        return peers;
    }

    public void setPeers(Set<String> peers) {
        this.peers = peers == null ? new HashSet<>() : new HashSet<>(peers);
    }

    public RaftGroupConfig merge(RaftGroupConfig other) {
        Objects.requireNonNull(other, "other must not be null");
        RaftGroupConfig merged = new RaftGroupConfig();
        merged.setGroupId(other.getGroupId() != null ? other.getGroupId() : groupId);
        Set<String> mergedPeers = new HashSet<>(peers);
        mergedPeers.addAll(other.getPeers());
        merged.setPeers(mergedPeers);
        return merged;
    }
}
