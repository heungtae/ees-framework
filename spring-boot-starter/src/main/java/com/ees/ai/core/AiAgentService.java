package com.ees.ai.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AiAgentService {

    Mono<AiResponse> chat(AiRequest request);

    default Flux<AiResponse> chatStream(AiRequest request) {
        return chat(request).flux();
    }
}
