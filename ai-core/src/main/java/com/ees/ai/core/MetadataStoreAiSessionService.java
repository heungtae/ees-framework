package com.ees.ai.core;

import com.ees.metadatastore.MetadataStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import reactor.core.publisher.Mono;

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
    public Mono<AiSession> load(String sessionId) {
        return metadataStore.get(key(sessionId), StoredSession.class)
            .map(optional -> optional
                .map(record -> new AiSession(sessionId, record.messages(), record.updatedAt()))
                .orElseGet(() -> new AiSession(sessionId, List.of(), Instant.now())));
    }

    @Override
    public Mono<AiSession> append(String sessionId, AiMessage message) {
        Instant now = Instant.now();
        return metadataStore.get(key(sessionId), StoredSession.class)
            .defaultIfEmpty(Optional.empty())
            .flatMap(optional -> {
                List<AiMessage> messages = optional.map(StoredSession::messages)
                    .map(ArrayList::new)
                    .orElseGet(ArrayList::new);
                messages.add(message);
                StoredSession updated = new StoredSession(messages, now);
                return metadataStore.put(key(sessionId), updated, ttl)
                    .thenReturn(new AiSession(sessionId, messages, now));
            });
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
