package com.ees.cluster.raft.snapshot;

import com.ees.cluster.state.ClusterStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Stores snapshots inside the configured {@link ClusterStateRepository}. Used for both DB and
 * Kafka KTable backends.
 */
public class RepositoryClusterSnapshotStore implements ClusterSnapshotStore {

    private static final Logger log = LoggerFactory.getLogger(RepositoryClusterSnapshotStore.class);
    private static final String PREFIX = "cluster:raft/snapshots/";

    private final ClusterStateRepository repository;

    public RepositoryClusterSnapshotStore(ClusterStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ClusterSnapshot> loadLatest(String groupId) throws IOException {
        Optional<byte[]> blob = repository.get(key(groupId), byte[].class)
                .blockOptional()
                .orElse(Optional.empty());
        if (blob.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ClusterSnapshotCodec.deserialize(blob.get()));
    }

    @Override
    public void persist(ClusterSnapshot snapshot) throws IOException {
        byte[] data = ClusterSnapshotCodec.serialize(snapshot);
        Mono<Boolean> write = repository.put(key(snapshot.groupId()), data, Duration.ZERO);
        write.blockOptional();
        log.info("Persisted snapshot for group {} to repository (term={}, index={}, takenAt={})",
                snapshot.groupId(), snapshot.term(), snapshot.index(), snapshot.takenAt());
    }

    private String key(String groupId) {
        return PREFIX + groupId;
    }
}
