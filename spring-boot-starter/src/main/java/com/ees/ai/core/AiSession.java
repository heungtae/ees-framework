package com.ees.ai.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AiSession(String sessionId, List<AiMessage> messages, Instant updatedAt) {

    public AiSession {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (messages == null) {
            messages = Collections.emptyList();
        } else {
            messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
