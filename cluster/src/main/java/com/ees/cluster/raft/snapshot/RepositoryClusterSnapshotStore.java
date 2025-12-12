package com.ees.cluster.raft.snapshot;

import com.ees.cluster.state.ClusterStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Stores snapshots inside the configured {@link ClusterStateRepository}. Used for both DB and
 * Kafka KTable backends.
 */
public class RepositoryClusterSnapshotStore implements ClusterSnapshotStore {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(RepositoryClusterSnapshotStore.class);
    private static final String PREFIX = "cluster:raft/snapshots/";

    private final ClusterStateRepository repository;
    /**
     * 인스턴스를 생성한다.
     * @param repository 
     */

    public RepositoryClusterSnapshotStore(ClusterStateRepository repository) {
        this.repository = repository;
    }
    /**
     * loadLatest를 수행한다.
     * @param groupId 
     * @return 
     */

    @Override
    public Optional<ClusterSnapshot> loadLatest(String groupId) throws IOException {
        Optional<byte[]> blob = repository.get(key(groupId), byte[].class);
        if (blob.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ClusterSnapshotCodec.deserialize(blob.get()));
    }
    /**
     * persist를 수행한다.
     * @param snapshot 
     */

    @Override
    public void persist(ClusterSnapshot snapshot) throws IOException {
        byte[] data = ClusterSnapshotCodec.serialize(snapshot);
        repository.put(key(snapshot.groupId()), data, Duration.ZERO);
        log.info("Persisted snapshot for group {} to repository (term={}, index={}, takenAt={})",
                snapshot.groupId(), snapshot.term(), snapshot.index(), snapshot.takenAt());
    }
    // key 동작을 수행한다.

    private String key(String groupId) {
        return PREFIX + groupId;
    }
}
