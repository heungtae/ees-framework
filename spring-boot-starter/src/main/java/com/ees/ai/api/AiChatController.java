package com.ees.ai.api;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiRequest;
import com.ees.ai.core.AiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebFlux endpoints for AI chat (sync + streaming).
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiAgentService aiAgentService;

    public AiChatController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @PostMapping("/chat")
    public Mono<AiResponse> chat(@RequestBody AiRequest request) {
        return aiAgentService.chat(request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiResponse>> chatStream(@RequestBody AiRequest request) {
        AiRequest streamingRequest = request.streaming()
            ? request
            : new AiRequest(request.sessionId(), request.userId(), request.messages(), request.toolsAllowed(), true);
        return aiAgentService.chatStream(streamingRequest)
            .map(response -> ServerSentEvent.builder(response)
                .id(response.sessionId())
                .event(response.streaming() ? "chunk" : "complete")
                .build());
    }
}
