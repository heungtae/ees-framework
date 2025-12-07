package com.ees.ai.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import reactor.core.publisher.Mono;

public class InMemoryAiSessionService implements AiSessionService {

    private final Map<String, List<AiMessage>> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<AiSession> load(String sessionId) {
        return Mono.fromSupplier(() -> new AiSession(sessionId, sessions.getOrDefault(sessionId, List.of()), Instant.now()));
    }

    @Override
    public Mono<AiSession> append(String sessionId, AiMessage message) {
        return Mono.fromSupplier(() -> {
            sessions.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>()).add(message);
            return new AiSession(sessionId, sessions.get(sessionId), Instant.now());
        });
    }
}
