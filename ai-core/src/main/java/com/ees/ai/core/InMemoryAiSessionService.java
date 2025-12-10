package com.ees.ai.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryAiSessionService implements AiSessionService {

    private final Map<String, List<AiMessage>> sessions = new ConcurrentHashMap<>();

    @Override
    public AiSession load(String sessionId) {
        return new AiSession(sessionId, sessions.getOrDefault(sessionId, List.of()), Instant.now());
    }

    @Override
    public AiSession append(String sessionId, AiMessage message) {
        sessions.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>()).add(message);
        return new AiSession(sessionId, sessions.get(sessionId), Instant.now());
    }
}
