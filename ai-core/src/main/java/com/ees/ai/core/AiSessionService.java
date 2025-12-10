package com.ees.ai.core;

public interface AiSessionService {

    AiSession load(String sessionId);

    AiSession append(String sessionId, AiMessage message);
}
