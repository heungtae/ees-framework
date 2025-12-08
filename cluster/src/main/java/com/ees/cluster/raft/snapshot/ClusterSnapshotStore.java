package com.ees.cluster.raft.snapshot;

import java.io.IOException;
import java.util.Optional;

public interface ClusterSnapshotStore {

    Optional<ClusterSnapshot> loadLatest(String groupId) throws IOException;

    void persist(ClusterSnapshot snapshot) throws IOException;
}
