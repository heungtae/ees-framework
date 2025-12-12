package com.ees.metadatastore;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for MetadataStore implementations.
 */
abstract class AbstractMetadataStoreContractTest {

    protected abstract MetadataStore createStore(TestMutableClock clock) throws Exception;

    @Test
    void putGetAndDelete() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        MetadataStore store = createStore(clock);

        store.put("k1", "v1", Duration.ofSeconds(5));
        assertThat(store.get("k1", String.class)).contains("v1");

        assertThat(store.delete("k1")).isTrue();
        assertThat(store.get("k1", String.class)).isEmpty();
    }

    @Test
    void compareAndSetRespectsExpected() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        MetadataStore store = createStore(clock);

        store.put("k2", "old", Duration.ofSeconds(5));
        assertThat(store.compareAndSet("k2", "old", "new", Duration.ofSeconds(5))).isTrue();
        assertThat(store.get("k2", String.class)).contains("new");
        assertThat(store.compareAndSet("k2", "old", "x", Duration.ofSeconds(5))).isFalse();
    }

    @Test
    void scanReturnsPrefixMatches() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        MetadataStore store = createStore(clock);

        store.put("p/a", "1", Duration.ofSeconds(5));
        store.put("p/b", "2", Duration.ofSeconds(5));
        store.put("q/c", "3", Duration.ofSeconds(5));

        List<String> values = store.scan("p/", String.class);
        assertThat(values).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void watchReceivesEvents() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        MetadataStore store = createStore(clock);
        List<MetadataStoreEvent> events = new ArrayList<>();

        store.watch("watch/", events::add);
        store.put("watch/a", "v", Duration.ofSeconds(5));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).type()).isEqualTo(MetadataStoreEventType.PUT);
    }

    @Test
    void ttlExpires() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        MetadataStore store = createStore(clock);

        store.put("ttl/key", "v", Duration.ofSeconds(1));
        assertThat(store.get("ttl/key", String.class)).contains("v");

        clock.advance(Duration.ofSeconds(2));
        Optional<String> value = store.get("ttl/key", String.class);
        assertThat(value).isEmpty();
    }

    @Test
    void supportsRaftSnapshotKeyPattern() throws Exception {
        TestMutableClock clock = TestMutableClock.fixedNow();
        MetadataStore store = createStore(clock);
        String key = "cluster:raft/snapshots/group-1";

        store.put(key, "snapshot", Duration.ofSeconds(5));

        assertThat(store.get(key, String.class)).contains("snapshot");
        assertThat(store.scan("cluster:raft/snapshots/", String.class)).contains("snapshot");
    }
}
