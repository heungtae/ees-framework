package com.ees.ai.api;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiRequest;
import com.ees.ai.core.AiResponse;
import com.ees.ai.mcp.McpClient;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * WebFlux endpoints for AI chat (sync + streaming).
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiAgentService aiAgentService;
    private final McpClient mcpClient;

    private static final Set<String> DANGEROUS_TOOLS = Set.of(
        "cancelWorkflow", "pauseWorkflow", "resumeWorkflow", "assignKey", "releaseLock", "startWorkflow"
    );

    public AiChatController(AiAgentService aiAgentService, ObjectProvider<McpClient> mcpClient) {
        this.aiAgentService = aiAgentService;
        this.mcpClient = mcpClient.getIfAvailable();
    }

    @PostMapping("/chat")
    public AiResponse chat(@RequestBody AiRequest request,
                           @RequestHeader(name = "X-AI-Approve", defaultValue = "false") boolean approved) {
        AiRequest normalized = normalize(request, false, approved);
        return aiAgentService.chat(normalized);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiRequest request,
                                 @RequestHeader(name = "X-AI-Approve", defaultValue = "false") boolean approved) {
        AiRequest normalized = normalize(request, true, approved);
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());
        new Thread(() -> {
            try {
                List<AiResponse> stream = aiAgentService.chatStream(normalized);
                for (AiResponse response : stream) {
                    emitter.send(SseEmitter.event()
                        .id(response.sessionId())
                        .name(response.streaming() ? "chunk" : "complete")
                        .data(response));
                }
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }).start();
        return emitter;
    }

    @GetMapping("/resources/nodes")
    public ResponseEntity<String> listNodes() {
        requireMcp();
        return ResponseEntity.ok(mcpClient.listNodes());
    }

    @GetMapping("/resources/topology")
    public ResponseEntity<String> topology() {
        requireMcp();
        return ResponseEntity.ok(mcpClient.describeTopology());
    }

    private AiRequest normalize(AiRequest request, boolean forceStreaming, boolean approved) {
        if (request == null || isBlank(request.sessionId())) {
            throw new ResponseStatusException(BAD_REQUEST, "sessionId is required");
        }
        if (request.messages() == null || request.messages().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "at least one message is required");
        }
        List<String> toolsAllowed = request.toolsAllowed() == null ? Collections.emptyList() : request.toolsAllowed().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
        validateDangerousTools(toolsAllowed, approved);

        List<com.ees.ai.core.AiMessage> sanitizedMessages = request.messages().stream()
            .filter(Objects::nonNull)
            .filter(msg -> StringUtils.hasText(msg.content()))
            .map(msg -> new com.ees.ai.core.AiMessage(msg.role(), msg.content()))
            .toList();
        if (sanitizedMessages.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "message content is required");
        }

        return new AiRequest(
            request.sessionId(),
            StringUtils.hasText(request.userId()) ? request.userId() : "anonymous",
            sanitizedMessages,
            toolsAllowed,
            forceStreaming || request.streaming()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateDangerousTools(List<String> toolsAllowed, boolean approved) {
        if (toolsAllowed.isEmpty()) {
            return;
        }
        boolean hasDanger = toolsAllowed.stream().anyMatch(DANGEROUS_TOOLS::contains);
        if (hasDanger && !approved) {
            throw new ResponseStatusException(BAD_REQUEST, "Dangerous tools require explicit approval header (X-AI-Approve: true)");
        }
    }

    private void requireMcp() {
        if (mcpClient == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "MCP client not configured");
        }
    }
}
