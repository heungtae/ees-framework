package com.ees.metadatastore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * File-based metadata store for single-node persistence.
 */
public class FileMetadataStore implements MetadataStore {

    private final Path baseDir;
    private final MetadataSerializer serializer;
    private final Clock clock;
    private final Map<String, StoredValue> cache = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<MetadataStoreEvent>> listeners = new CopyOnWriteArrayList<>();
    /**
     * 인스턴스를 생성한다.
     * @param baseDir 
     */

    public FileMetadataStore(Path baseDir) {
        this(baseDir, new JsonMetadataSerializer(), Clock.systemUTC());
    }
    /**
     * 인스턴스를 생성한다.
     * @param baseDir 
     * @param serializer 
     * @param clock 
     */

    public FileMetadataStore(Path baseDir, MetadataSerializer serializer, Clock clock) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create metadata store directory: " + baseDir, e);
        }
    }
    /**
     * put를 수행한다.
     * @param key 
     * @param value 
     * @param ttl 
     * @return 
     */

    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        Instant expiresAt = MetadataTtlUtils.expiresAt(clock, ttl);
        StoredValue stored = new StoredValue(serializer.serialize(value), expiresAt);
        writeToDisk(key, stored);
        cache.put(key, stored);
        publishEvent(key, MetadataStoreEventType.PUT, value);
        return true;
    }
    /**
     * putIfAbsent를 수행한다.
     * @param key 
     * @param value 
     * @param ttl 
     * @return 
     */

    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Optional<T> existing = get(key, (Class<T>) value.getClass());
        if (existing.isPresent()) {
            return false;
        }
        return put(key, value, ttl);
    }
    /**
     * get를 수행한다.
     * @param key 
     * @param type 
     * @return 
     */

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        StoredValue stored = cache.computeIfAbsent(key, this::readFromDisk);
        if (stored == null || MetadataTtlUtils.isExpired(clock, stored.expiresAt())) {
            delete(key);
            return Optional.empty();
        }
        T value = serializer.deserialize(stored.bytes(), type);
        return Optional.ofNullable(value);
    }
    /**
     * delete를 수행한다.
     * @param key 
     * @return 
     */

    @Override
    public boolean delete(String key) {
        Objects.requireNonNull(key, "key must not be null");
        StoredValue removed = cache.remove(key);
        try {
            Files.deleteIfExists(pathForKey(key));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete metadata file for key " + key, e);
        }
        if (removed != null) {
            publishEvent(key, MetadataStoreEventType.DELETE, serializer.deserialize(removed.bytes(), Object.class));
        }
        return removed != null;
    }
    /**
     * compareAndSet를 수행한다.
     * @param key 
     * @param expectedValue 
     * @param newValue 
     * @param ttl 
     * @return 
     */

    @Override
    public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(expectedValue, "expectedValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        cleanExpired();
        Optional<T> current = get(key, (Class<T>) expectedValue.getClass());
        if (current.isEmpty() || !Objects.equals(current.get(), expectedValue)) {
            return false;
        }
        return put(key, newValue, ttl);
    }
    /**
     * scan를 수행한다.
     * @param prefix 
     * @param type 
     * @return 
     */

    @Override
    public <T> List<T> scan(String prefix, Class<T> type) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(type, "type must not be null");
        cleanExpired();
        try {
            return Files.list(baseDir)
                .filter(path -> path.getFileName().toString().startsWith(safeName(prefix)))
                .map(this::readStoredValue)
                .filter(Objects::nonNull)
                .filter(value -> !MetadataTtlUtils.isExpired(clock, value.expiresAt()))
                .map(stored -> serializer.deserialize(stored.bytes(), type))
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan metadata files", e);
        }
    }
    /**
     * watch를 수행한다.
     * @param prefix 
     * @param consumer 
     */

    @Override
    public void watch(String prefix, Consumer<MetadataStoreEvent> consumer) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Predicate<MetadataStoreEvent> filter = event -> event.key().startsWith(prefix);
        listeners.add(event -> {
            if (filter.test(event)) {
                consumer.accept(event);
            }
        });
    }
    // cleanExpired 동작을 수행한다.

    private void cleanExpired() {
        try {
            Files.list(baseDir).forEach(path -> {
                StoredValue value = readStoredValue(path);
                if (value != null && MetadataTtlUtils.isExpired(clock, value.expiresAt())) {
                    try {
                        Files.deleteIfExists(path);
                        cache.remove(keyFromPath(path));
                        publishEvent(keyFromPath(path), MetadataStoreEventType.EXPIRE, serializer.deserialize(value.bytes(), Object.class), clock.instant());
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to delete expired metadata file: " + path, e);
                    }
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan metadata directory for expiration", e);
        }
    }
    // writeToDisk 동작을 수행한다.

    private void writeToDisk(String key, StoredValue stored) {
        try {
            String header = stored.expiresAt() == null ? "-" : stored.expiresAt().toString();
            byte[] payload = stored.bytes();
            byte[] data = (header + "\n").getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[data.length + payload.length];
            System.arraycopy(data, 0, combined, 0, data.length);
            System.arraycopy(payload, 0, combined, data.length, payload.length);
            Files.write(pathForKey(key), combined);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write metadata for key " + key, e);
        }
    }
    // readFromDisk 동작을 수행한다.

    private StoredValue readFromDisk(String key) {
        return readStoredValue(pathForKey(key));
    }
    // readStoredValue 동작을 수행한다.

    private StoredValue readStoredValue(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            byte[] all = Files.readAllBytes(path);
            String content = new String(all, StandardCharsets.UTF_8);
            int newlineIndex = content.indexOf('\n');
            if (newlineIndex < 0) {
                return null;
            }
            String header = content.substring(0, newlineIndex);
            String payloadStr = content.substring(newlineIndex + 1);
            Instant expiresAt = "-".equals(header) ? null : Instant.parse(header);
            byte[] payload = payloadStr.getBytes(StandardCharsets.UTF_8);
            return new StoredValue(payload, expiresAt);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read metadata from " + path, e);
        }
    }
    // pathForKey 동작을 수행한다.

    private Path pathForKey(String key) {
        return baseDir.resolve(safeName(key));
    }
    // safeName 동작을 수행한다.

    private String safeName(String key) {
        return key.replaceAll("[^a-zA-Z0-9\\-_.:]", "_");
    }
    // keyFromPath 동작을 수행한다.

    private String keyFromPath(Path path) {
        return path.getFileName().toString();
    }
    // publishEvent 동작을 수행한다.

    private void publishEvent(String key, MetadataStoreEventType type, Object value) {
        publishEvent(key, type, value, clock.instant());
    }
    // publishEvent 동작을 수행한다.

    private void publishEvent(String key, MetadataStoreEventType type, Object value, Instant when) {
        MetadataStoreEvent event = new MetadataStoreEvent(key, type, Optional.ofNullable(value), when);
        for (Consumer<MetadataStoreEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private record StoredValue(byte[] bytes, Instant expiresAt) {
    }
}
