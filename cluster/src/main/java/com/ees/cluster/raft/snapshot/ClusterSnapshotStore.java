package com.ees.cluster.raft.snapshot;

import java.io.IOException;
import java.util.Optional;

/**
 * Raft 클러스터 스냅샷을 저장/로드하는 저장소 추상화.
 */
public interface ClusterSnapshotStore {

    /**
     * 그룹의 최신 스냅샷을 로드한다.
     *
     * @param groupId 그룹 ID
     * @return 최신 스냅샷(없으면 empty)
     * @throws IOException 로드 실패 시
     */
    Optional<ClusterSnapshot> loadLatest(String groupId) throws IOException;

    /**
     * 스냅샷을 저장한다.
     *
     * @param snapshot 스냅샷
     * @throws IOException 저장 실패 시
     */
    void persist(ClusterSnapshot snapshot) throws IOException;
}
