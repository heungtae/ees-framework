package com.ees.metadatastore;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Kafka Streams KTable 기반 MetadataStore 구현. compacted 토픽을 사용해 키/값을 영속화하고
 * materialized state store를 통해 조회/스캔/워치를 제공한다. 테스트 환경에서는 TopologyTestDriver로
 * 인메모리 모드로 동작한다.
 */
public class KafkaKTableMetadataStore implements MetadataStore, Closeable {

    private static final String DEFAULT_STORE_NAME = "metadata-store-ktable";

    private final String topic;
    private final MetadataSerializer serializer;
    private final Clock clock;
    private final Serde<ValueWithTtl> valueSerde = new ValueWithTtlSerde();
    private final CopyOnWriteArrayList<Consumer<MetadataStoreEvent>> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> pendingExpiryKeys = ConcurrentHashMap.newKeySet();
    private final String storeName;
    private final StateAdapter stateAdapter;

    public KafkaKTableMetadataStore(Properties streamsConfig, String topic) {
        this(streamsConfig, topic, new JsonMetadataSerializer(), Clock.systemUTC(), false);
    }

    public KafkaKTableMetadataStore(Properties streamsConfig, String topic, MetadataSerializer serializer, Clock clock) {
        this(streamsConfig, topic, serializer, clock, false);
    }

    /**
     * 테스트 전용 인메모리 드라이버를 사용하는 팩토리.
     */
    public static KafkaKTableMetadataStore inMemoryForTests(Properties streamsConfig, String topic, MetadataSerializer serializer, Clock clock) {
        return new KafkaKTableMetadataStore(streamsConfig, topic, serializer, clock, true);
    }

    private KafkaKTableMetadataStore(Properties streamsConfig, String topic, MetadataSerializer serializer, Clock clock, boolean useTestDriver) {
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.storeName = DEFAULT_STORE_NAME + "-" + topic;
        ensureTopicNameValid(topic);

        Topology topology = buildTopology(topic);
        if (useTestDriver) {
            TopologyTestDriver driver = new TopologyTestDriver(topology, streamsConfig);
            KeyValueStore<String, ValueWithTtl> kvStore = driver.getKeyValueStore(storeName);
            TestInputTopic<String, ValueWithTtl> inputTopic = driver.createInputTopic(topic, new StringSerializer(), new ValueWithTtlSerializer());
            this.stateAdapter = new TestDriverStateAdapter(driver, kvStore, inputTopic);
        } else {
            KafkaStreams streams = new KafkaStreams(topology, streamsConfig);
            streams.start();
            ReadOnlyKeyValueStore<String, ValueWithTtl> kvStore = waitForStore(streams, storeName);
            KafkaProducer<String, ValueWithTtl> producer = buildProducer(streamsConfig);
            this.stateAdapter = new KafkaStreamsStateAdapter(streams, kvStore, producer, topic);
        }
    }

    @Override
    public <T> boolean put(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        purgeExpired();
        ValueWithTtl stored = new ValueWithTtl(serializer.serialize(value), toEpochMillis(MetadataTtlUtils.expiresAt(clock, ttl)));
        stateAdapter.send(key, stored);
        return true;
    }

    @Override
    public <T> boolean putIfAbsent(String key, T value, Duration ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        purgeExpired();
        Optional<T> current = get(key, (Class<T>) value.getClass());
        if (current.isPresent()) {
            return false;
        }
        return put(key, value, ttl);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        purgeExpired();
        ValueWithTtl stored = stateAdapter.get(key);
        if (stored == null) {
            return Optional.empty();
        }
        if (MetadataTtlUtils.isExpired(clock, stored.expiresAtInstant())) {
            sendExpireTombstone(key, stored);
            return Optional.empty();
        }
        T value = serializer.deserialize(stored.payload(), type);
        return Optional.ofNullable(value);
    }

    @Override
    public boolean delete(String key) {
        Objects.requireNonNull(key, "key must not be null");
        purgeExpired();
        ValueWithTtl stored = stateAdapter.get(key);
        if (stored == null) {
            return false;
        }
        stateAdapter.send(key, null);
        return true;
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
        List<T> results = new ArrayList<>();
        try (KeyValueIterator<String, ValueWithTtl> iterator = stateAdapter.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, ValueWithTtl> kv = iterator.next();
                if (!kv.key.startsWith(prefix) || kv.value == null) {
                    continue;
                }
                if (MetadataTtlUtils.isExpired(clock, kv.value.expiresAtInstant())) {
                    sendExpireTombstone(kv.key, kv.value);
                    continue;
                }
                results.add(serializer.deserialize(kv.value.payload(), type));
            }
        }
        return results;
    }

    @Override
    public void watch(String prefix, Consumer<MetadataStoreEvent> consumer) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        java.util.function.Predicate<MetadataStoreEvent> filter = event -> event.key().startsWith(prefix);
        listeners.add(event -> {
            if (filter.test(event)) {
                consumer.accept(event);
            }
        });
    }

    @Override
    public void close() {
        stateAdapter.close();
    }

    private void handleStreamEvent(String key, ValueWithTtl valueWithTtl) {
        MetadataStoreEventType eventType;
        Object decodedValue = null;
        if (valueWithTtl == null) {
            eventType = pendingExpiryKeys.remove(key) ? MetadataStoreEventType.EXPIRE : MetadataStoreEventType.DELETE;
        } else if (MetadataTtlUtils.isExpired(clock, valueWithTtl.expiresAtInstant())) {
            eventType = MetadataStoreEventType.EXPIRE;
            decodedValue = serializer.deserialize(valueWithTtl.payload(), Object.class);
        } else {
            eventType = MetadataStoreEventType.PUT;
            decodedValue = serializer.deserialize(valueWithTtl.payload(), Object.class);
        }
        publishEvent(key, eventType, decodedValue, clock.instant());
    }

    private void sendExpireTombstone(String key, ValueWithTtl stored) {
        pendingExpiryKeys.add(key);
        stateAdapter.send(key, null);
    }

    private void purgeExpired() {
        try (KeyValueIterator<String, ValueWithTtl> iterator = stateAdapter.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, ValueWithTtl> kv = iterator.next();
                if (kv.value != null && MetadataTtlUtils.isExpired(clock, kv.value.expiresAtInstant())) {
                    sendExpireTombstone(kv.key, kv.value);
                }
            }
        }
    }

    private Topology buildTopology(String topic) {
        StreamsBuilder builder = new StreamsBuilder();
        KTable<String, ValueWithTtl> table = builder.stream(topic, Consumed.with(Serdes.String(), valueSerde))
            .peek(this::handleStreamEvent)
            .toTable(Materialized.<String, ValueWithTtl>as(Stores.persistentKeyValueStore(storeName))
                .withKeySerde(Serdes.String())
                .withValueSerde(valueSerde));
        // Touch table to ensure materialization happens (builder requires usage).
        table.queryableStoreName();
        return builder.build();
    }

    private ReadOnlyKeyValueStore<String, ValueWithTtl> waitForStore(KafkaStreams streams, String storeName) {
        int attempts = 0;
        while (attempts < 30) {
            try {
                return streams.store(org.apache.kafka.streams.StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore()));
            } catch (InvalidStateStoreException e) {
                attempts++;
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Kafka Streams store", ie);
                }
            }
        }
        throw new IllegalStateException("Kafka Streams state store is not ready");
    }

    private KafkaProducer<String, ValueWithTtl> buildProducer(Properties streamsConfig) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, streamsConfig.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ValueWithTtlSerializer.class);
        return new KafkaProducer<>(producerProps);
    }

    private void publishEvent(String key, MetadataStoreEventType type, Object value, Instant when) {
        MetadataStoreEvent event = new MetadataStoreEvent(key, type, Optional.ofNullable(value), when);
        for (Consumer<MetadataStoreEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    private Long toEpochMillis(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toEpochMilli();
    }

    private void ensureTopicNameValid(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidTopicException("Topic name must not be blank");
        }
    }

    private interface StateAdapter {
        ValueWithTtl get(String key);

        KeyValueIterator<String, ValueWithTtl> all();

        void send(String key, ValueWithTtl value);

        default void close() {
        }
    }

    private final class KafkaStreamsStateAdapter implements StateAdapter {

        private final KafkaStreams streams;
        private final ReadOnlyKeyValueStore<String, ValueWithTtl> store;
        private final KafkaProducer<String, ValueWithTtl> producer;
        private final String topic;

        KafkaStreamsStateAdapter(KafkaStreams streams, ReadOnlyKeyValueStore<String, ValueWithTtl> store,
                                 KafkaProducer<String, ValueWithTtl> producer, String topic) {
            this.streams = streams;
            this.store = store;
            this.producer = producer;
            this.topic = topic;
        }

        @Override
        public ValueWithTtl get(String key) {
            return store.get(key);
        }

        @Override
        public KeyValueIterator<String, ValueWithTtl> all() {
            return store.all();
        }

        @Override
        public void send(String key, ValueWithTtl value) {
            try {
                producer.send(new ProducerRecord<>(topic, key, value)).get();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to publish metadata to Kafka topic", e);
            }
        }

        @Override
        public void close() {
            producer.close();
            streams.close();
        }
    }

    private final class TestDriverStateAdapter implements StateAdapter {

        private final TopologyTestDriver driver;
        private final KeyValueStore<String, ValueWithTtl> store;
        private final TestInputTopic<String, ValueWithTtl> inputTopic;

        TestDriverStateAdapter(TopologyTestDriver driver, KeyValueStore<String, ValueWithTtl> store,
                               TestInputTopic<String, ValueWithTtl> inputTopic) {
            this.driver = driver;
            this.store = store;
            this.inputTopic = inputTopic;
        }

        @Override
        public ValueWithTtl get(String key) {
            return store.get(key);
        }

        @Override
        public KeyValueIterator<String, ValueWithTtl> all() {
            return store.all();
        }

        @Override
        public void send(String key, ValueWithTtl value) {
            inputTopic.pipeInput(key, value, clock.instant());
        }

        @Override
        public void close() {
            driver.close();
        }
    }

    private record ValueWithTtl(byte[] payload, Long expiresAtEpochMilli) {
        Instant expiresAtInstant() {
            if (expiresAtEpochMilli == null) {
                return null;
            }
            return Instant.ofEpochMilli(expiresAtEpochMilli);
        }
    }

    private static final class ValueWithTtlSerializer implements Serializer<ValueWithTtl> {

        @Override
        public byte[] serialize(String topic, ValueWithTtl data) {
            if (data == null) {
                return null;
            }
            int payloadLength = data.payload() == null ? 0 : data.payload().length;
            int total = Integer.BYTES + payloadLength + Long.BYTES;
            ByteBuffer buffer = ByteBuffer.allocate(total);
            buffer.putInt(payloadLength);
            if (payloadLength > 0) {
                buffer.put(data.payload());
            }
            buffer.putLong(data.expiresAtEpochMilli() == null ? -1L : data.expiresAtEpochMilli());
            return buffer.array();
        }
    }

    private static final class ValueWithTtlSerde implements Serde<ValueWithTtl> {

        @Override
        public Serializer<ValueWithTtl> serializer() {
            return new ValueWithTtlSerializer();
        }

        @Override
        public org.apache.kafka.common.serialization.Deserializer<ValueWithTtl> deserializer() {
            return (topic, bytes) -> {
                if (bytes == null) {
                    return null;
                }
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                int payloadLength = buffer.getInt();
                byte[] payload = null;
                if (payloadLength > 0) {
                    payload = new byte[payloadLength];
                    buffer.get(payload);
                }
                long expiresRaw = buffer.getLong();
                Long expiresAt = expiresRaw < 0 ? null : expiresRaw;
                return new ValueWithTtl(payload, expiresAt);
            };
        }
    }
}
