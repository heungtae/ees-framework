package com.ees.ai.core;

import reactor.core.publisher.Mono;

public interface AiSessionService {

    Mono<AiSession> load(String sessionId);

    Mono<AiSession> append(String sessionId, AiMessage message);
}
