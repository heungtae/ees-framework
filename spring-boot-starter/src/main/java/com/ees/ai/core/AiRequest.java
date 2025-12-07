package com.ees.ai.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AiRequest(String sessionId,
                        String userId,
                        List<AiMessage> messages,
                        List<String> toolsAllowed,
                        boolean streaming) {

    public AiRequest {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (messages == null) {
            messages = Collections.emptyList();
        } else {
            messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
        if (toolsAllowed == null) {
            toolsAllowed = Collections.emptyList();
        } else {
            toolsAllowed = Collections.unmodifiableList(new ArrayList<>(toolsAllowed));
        }
    }
}
