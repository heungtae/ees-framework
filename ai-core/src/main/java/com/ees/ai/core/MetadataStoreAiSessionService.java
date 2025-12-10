package com.ees.ai.core;

import com.ees.metadatastore.MetadataStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AiSessionService backed by MetadataStore for persistence.
 */
public class MetadataStoreAiSessionService implements AiSessionService {

    private static final String KEY_PREFIX = "ai:sessions:";

    private final MetadataStore metadataStore;
    private final Duration ttl;

    public MetadataStoreAiSessionService(MetadataStore metadataStore, Duration ttl) {
        this.metadataStore = metadataStore;
        this.ttl = ttl == null ? Duration.ZERO : ttl;
    }

    @Override
    public AiSession load(String sessionId) {
        Optional<StoredSession> stored = metadataStore.get(key(sessionId), StoredSession.class);
        return stored
            .map(record -> new AiSession(sessionId, record.messages(), record.updatedAt()))
            .orElseGet(() -> new AiSession(sessionId, List.of(), Instant.now()));
    }

    @Override
    public AiSession append(String sessionId, AiMessage message) {
        Instant now = Instant.now();
        Optional<StoredSession> optional = metadataStore.get(key(sessionId), StoredSession.class);
        List<AiMessage> messages = optional.map(StoredSession::messages)
            .map(ArrayList::new)
            .orElseGet(ArrayList::new);
        messages.add(message);
        StoredSession updated = new StoredSession(messages, now);
        metadataStore.put(key(sessionId), updated, ttl);
        return new AiSession(sessionId, messages, now);
    }

    private String key(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        return KEY_PREFIX + sessionId;
    }

    private record StoredSession(List<AiMessage> messages, Instant updatedAt) {
    }
}
