package com.ees.ai.workflow;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiMessage;
import com.ees.ai.core.AiRequest;
import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMeta;
import com.ees.framework.pipeline.PipelineStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@FxPipelineStep("ai-agent-step")
@Component
public class AiAgentPipelineStep implements PipelineStep<Object, Object> {

    static final String ATTR_TOOLS_ALLOWED = "aiToolsAllowed";
    static final String ATTR_PROMPT = "aiPrompt";
    static final String ATTR_RESPONSE = "aiResponse";
    static final String ATTR_SESSION = "aiSessionId";
    static final String ATTR_ERROR = "aiError";
    static final String PIPELINE_STEP = "ai-agent-step";

    private final AiAgentService aiAgentService;

    public AiAgentPipelineStep(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @Override
    public FxContext<Object> apply(FxContext<Object> context) {
        AiRequest request = buildRequest(context);

        try {
            com.ees.ai.core.AiResponse response = aiAgentService.chat(request);
            return attachResponse(context, response);
        } catch (Exception ex) {
            return attachError(context, ex);
        }
    }

    private AiRequest buildRequest(FxContext<Object> context) {
        Map<String, Object> attributes = context.meta().attributes();
        List<AiMessage> messages = new ArrayList<>();

        String prompt = attributeAsString(attributes, ATTR_PROMPT);
        if (prompt != null && !prompt.isBlank()) {
            messages.add(new AiMessage("system", prompt));
        }

        String payload = context.message() != null && context.message().payload() != null
            ? context.message().payload().toString()
            : "";
        messages.add(new AiMessage("user", payload));

        List<String> toolsAllowed = extractToolsAllowed(attributes);

        String sessionId = context.meta().sourceId() != null ? context.meta().sourceId() : context.command().name();
        String userId = context.headers().get("userId") != null ? context.headers().get("userId") : "workflow";

        return new AiRequest(
            sessionId,
            userId,
            messages,
            toolsAllowed,
            false
        );
    }

    private FxContext<Object> attachResponse(FxContext<Object> context, com.ees.ai.core.AiResponse response) {
        Map<String, Object> attributes = new HashMap<>(context.meta().attributes());
        attributes.put(ATTR_RESPONSE, response.content());
        attributes.put(ATTR_SESSION, response.sessionId());
        FxMeta meta = new FxMeta(
            context.meta().sourceId(),
            PIPELINE_STEP,
            context.meta().retries(),
            attributes
        );
        return new FxContext<>(context.command(), context.headers(), context.message(), meta, context.affinity());
    }

    private FxContext<Object> attachError(FxContext<Object> context, Throwable ex) {
        Map<String, Object> attributes = new HashMap<>(context.meta().attributes());
        attributes.put(ATTR_ERROR, ex.getMessage());
        FxMeta meta = new FxMeta(
            context.meta().sourceId(),
            PIPELINE_STEP,
            context.meta().retries() + 1,
            attributes
        );
        return new FxContext<>(context.command(), context.headers(), context.message(), meta, context.affinity());
    }

    private List<String> extractToolsAllowed(Map<String, Object> attributes) {
        Object value = attributes.get(ATTR_TOOLS_ALLOWED);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .toList();
        }
        if (value instanceof String str) {
            return List.of(str.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        }
        return List.of(value.toString());
    }

    private String attributeAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
}
