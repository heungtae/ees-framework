package com.ees.ai.api;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiRequest;
import com.ees.ai.core.AiResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
        AiRequest normalized = normalize(request, false);
        return aiAgentService.chat(normalized);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiResponse>> chatStream(@RequestBody AiRequest request) {
        AiRequest normalized = normalize(request, true);
        return aiAgentService.chatStream(normalized)
            .map(response -> ServerSentEvent.builder(response)
                .id(response.sessionId())
                .event(response.streaming() ? "chunk" : "complete")
                .build());
    }

    private AiRequest normalize(AiRequest request, boolean forceStreaming) {
        if (request == null || isBlank(request.sessionId())) {
            throw new ResponseStatusException(BAD_REQUEST, "sessionId is required");
        }
        List<String> toolsAllowed = request.toolsAllowed() == null ? Collections.emptyList() : request.toolsAllowed();
        return new AiRequest(
            request.sessionId(),
            request.userId(),
            request.messages(),
            toolsAllowed,
            forceStreaming || request.streaming()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
