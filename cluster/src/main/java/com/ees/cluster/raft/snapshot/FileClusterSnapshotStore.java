package com.ees.cluster.raft.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Stores snapshots on the local filesystem under a group-specific file.
 */
public class FileClusterSnapshotStore implements ClusterSnapshotStore {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(FileClusterSnapshotStore.class);

    private final Path baseDir;
    private final Clock clock;
    /**
     * 인스턴스를 생성한다.
     * @param baseDir 
     */

    public FileClusterSnapshotStore(Path baseDir) {
        this(baseDir, Clock.systemUTC());
    }
    /**
     * 인스턴스를 생성한다.
     * @param baseDir 
     * @param clock 
     */

    public FileClusterSnapshotStore(Path baseDir, Clock clock) {
        this.baseDir = baseDir;
        this.clock = clock;
    }
    /**
     * loadLatest를 수행한다.
     * @param groupId 
     * @return 
     */

    @Override
    public Optional<ClusterSnapshot> loadLatest(String groupId) throws IOException {
        Path path = snapshotPath(groupId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        byte[] data = Files.readAllBytes(path);
        ClusterSnapshot snapshot = ClusterSnapshotCodec.deserialize(data);
        return Optional.of(snapshot);
    }
    /**
     * persist를 수행한다.
     * @param snapshot 
     */

    @Override
    public void persist(ClusterSnapshot snapshot) throws IOException {
        Files.createDirectories(baseDir);
        Path path = snapshotPath(snapshot.groupId());
        byte[] data = ClusterSnapshotCodec.serialize(snapshot);
        Files.write(path, data);
        log.info("Persisted snapshot for group {} at {} (term={}, index={}, takenAt={})",
                snapshot.groupId(), path.toAbsolutePath(), snapshot.term(), snapshot.index(), snapshot.takenAt());
    }
    // snapshotPath 동작을 수행한다.

    private Path snapshotPath(String groupId) {
        String filename = groupId + ".snapshot.json";
        return baseDir.resolve(filename);
    }
}
