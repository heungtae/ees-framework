package com.ees.metadatastore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * JDBC-based MetadataStore.
 */
public class DbMetadataStore implements MetadataStore {

    private final DataSource dataSource;
    private final MetadataSerializer serializer;
    private final Clock clock;
    private final CopyOnWriteArrayList<Consumer<MetadataStoreEvent>> listeners = new CopyOnWriteArrayList<>();
    private final String table;

    public DbMetadataStore(DataSource dataSource, String table) {
        this(dataSource, table, new JsonMetadataSerializer(), Clock.systemUTC());
    }

    public DbMetadataStore(DataSource dataSource, String table, MetadataSerializer serializer, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.table = Objects.requireNonNull(table, "table must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        Instant expiresAt = MetadataTtlUtils.expiresAt(clock, ttl);
        byte[] payload = serializer.serialize(value);
        String sql = """
            MERGE INTO %s (id, payload, expires_at) KEY(id) VALUES (?, ?, ?)
            """.formatted(table);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setBytes(2, payload);
            if (expiresAt != null) {
                ps.setTimestamp(3, Timestamp.from(expiresAt));
            } else {
                ps.setTimestamp(3, null);
            }
            ps.executeUpdate();
            publishEvent(key, MetadataStoreEventType.PUT, value);
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to put metadata", e);
        }
    }

    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        Optional<T> existing = get(key, (Class<T>) value.getClass());
        if (existing.isPresent()) {
            return false;
        }
        return put(key, value, ttl);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        purgeExpired();
        String sql = "SELECT payload, expires_at FROM %s WHERE id = ?".formatted(table);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Timestamp expires = rs.getTimestamp("expires_at");
                if (expires != null && MetadataTtlUtils.isExpired(clock, expires.toInstant())) {
                    delete(key);
                    return Optional.empty();
                }
                byte[] payload = rs.getBytes("payload");
                T value = serializer.deserialize(payload, type);
                return Optional.ofNullable(value);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get metadata", e);
        }
    }

    @Override
    public boolean delete(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String sql = "DELETE FROM %s WHERE id = ?".formatted(table);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                publishEvent(key, MetadataStoreEventType.DELETE, null);
            }
            return updated > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete metadata", e);
        }
    }

    @Override
    public <T> boolean compareAndSet(String key, T expectedValue, T newValue, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(expectedValue, "expectedValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        Optional<T> current = get(key, (Class<T>) expectedValue.getClass());
        if (current.isEmpty() || !Objects.equals(current.get(), expectedValue)) {
            return false;
        }
        return put(key, newValue, ttl);
    }

    @Override
    public <T> List<T> scan(String prefix, Class<T> type) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(type, "type must not be null");
        purgeExpired();
        String sql = "SELECT id, payload, expires_at FROM %s WHERE id LIKE ?".formatted(table);
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp expires = rs.getTimestamp("expires_at");
                    if (expires != null && MetadataTtlUtils.isExpired(clock, expires.toInstant())) {
                        continue;
                    }
                    byte[] payload = rs.getBytes("payload");
                    T value = serializer.deserialize(payload, type);
                    results.add(value);
                }
            }
            return results;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to scan metadata", e);
        }
    }

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

    private void purgeExpired() {
        String sql = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= ?".formatted(table);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(clock.instant()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to purge expired metadata", e);
        }
    }

    private void publishEvent(String key, MetadataStoreEventType type, Object value) {
        MetadataStoreEvent event = new MetadataStoreEvent(key, type, Optional.ofNullable(value), clock.instant());
        for (Consumer<MetadataStoreEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
