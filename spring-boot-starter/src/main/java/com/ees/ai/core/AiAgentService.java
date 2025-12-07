package com.ees.ai.core;

import reactor.core.publisher.Mono;

public interface AiAgentService {

    Mono<AiResponse> chat(AiRequest request);
}
