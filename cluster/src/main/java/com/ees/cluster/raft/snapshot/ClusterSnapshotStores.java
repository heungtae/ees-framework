package com.ees.cluster.raft.snapshot;

import com.ees.cluster.raft.RaftServerConfig;
import com.ees.cluster.state.ClusterStateRepository;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory helpers to select a snapshot store based on {@link RaftServerConfig.SnapshotStore} settings.
 */
public final class ClusterSnapshotStores {

    private ClusterSnapshotStores() {
    }

    public static ClusterSnapshotStore forConfig(RaftServerConfig config, ClusterStateRepository repository) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        return switch (config.getSnapshotStore()) {
            case FILE -> new FileClusterSnapshotStore(Path.of(config.getDataDir(), "snapshots"));
            case DB, KAFKA_KTABLE -> new RepositoryClusterSnapshotStore(repository);
        };
    }
}
