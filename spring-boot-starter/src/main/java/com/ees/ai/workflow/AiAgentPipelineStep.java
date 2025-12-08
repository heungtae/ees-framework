package com.ees.ai.workflow;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiMessage;
import com.ees.ai.core.AiRequest;
import com.ees.framework.annotations.FxPipelineStep;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMeta;
import com.ees.framework.pipeline.PipelineStep;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FxPipelineStep("ai-agent-step")
@Component
public class AiAgentPipelineStep implements PipelineStep<Object, Object> {

    private final AiAgentService aiAgentService;

    public AiAgentPipelineStep(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @Override
    public Mono<FxContext<Object>> apply(FxContext<Object> context) {
        String payload = context.message() != null && context.message().payload() != null
            ? context.message().payload().toString()
            : "";
        AiRequest request = new AiRequest(
            context.meta().sourceId() != null ? context.meta().sourceId() : context.command().name(),
            "workflow",
            List.of(new AiMessage("user", payload)),
            List.of(),
            false
        );

        return aiAgentService.chat(request)
            .map(response -> {
                Map<String, Object> attributes = new HashMap<>(context.meta().attributes());
                attributes.put("aiResponse", response.content());
                FxMeta meta = new FxMeta(
                    context.meta().sourceId(),
                    "ai-agent-step",
                    context.meta().retries(),
                    attributes
                );
                return new FxContext<>(context.command(), context.headers(), context.message(), meta);
            })
            .onErrorResume(ex -> Mono.just(context));
    }
}
