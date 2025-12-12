package com.ees.cluster.raft;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Raft 그룹 구성 정보(그룹 ID 및 peer 목록).
 */
public class RaftGroupConfig {

    private String groupId;
    private Set<String> peers = new HashSet<>();

    /**
     * 빈 구성으로 생성한다.
     */
    public RaftGroupConfig() {
    }

    /**
     * groupId/peers를 지정해 생성한다.
     *
     * @param groupId 그룹 ID(옵션)
     * @param peers peer 목록(널이면 빈 집합)
     */
    public RaftGroupConfig(String groupId, Set<String> peers) {
        this.groupId = groupId;
        setPeers(peers);
    }

    /**
     * 그룹 ID를 반환한다.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * 그룹 ID를 설정한다.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * peer ID 목록을 반환한다.
     */
    public Set<String> getPeers() {
        return peers;
    }

    /**
     * peer ID 목록을 설정한다.
     *
     * @param peers peer ID 목록(널이면 빈 집합)
     */
    public void setPeers(Set<String> peers) {
        this.peers = peers == null ? new HashSet<>() : new HashSet<>(peers);
    }

    /**
     * 다른 구성과 병합한 새 인스턴스를 생성한다.
     * <p>
     * groupId는 other 값이 우선이며, peers는 합집합으로 병합한다.
     *
     * @param other 병합할 구성(널 불가)
     * @return 병합된 구성
     */
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
