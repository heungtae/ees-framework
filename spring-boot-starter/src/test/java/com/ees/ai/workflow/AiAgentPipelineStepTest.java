package com.ees.ai.workflow;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiRequest;
import com.ees.ai.core.AiResponse;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

class AiAgentPipelineStepTest {

    @Test
    void shouldAttachAiResponseToMeta() {
        AiAgentService stubService = new StubAiAgentService("decision");
        AiAgentPipelineStep step = new AiAgentPipelineStep(stubService);

        FxContext<Object> context = new FxContext<>(
            FxCommand.of("workflow-cmd"),
            FxHeaders.empty(),
            FxMessage.now("source", "input"),
            FxMeta.empty()
        );

        StepVerifier.create(step.apply(context))
            .assertNext(result -> {
                Assertions.assertThat(result.meta().attributes())
                    .containsEntry("aiResponse", "decision");
                Assertions.assertThat(result.meta().pipelineStep()).isEqualTo("ai-agent-step");
            })
            .verifyComplete();
    }

    private static class StubAiAgentService implements AiAgentService {

        private final String content;

        StubAiAgentService(String content) {
            this.content = content;
        }

        @Override
        public Mono<AiResponse> chat(AiRequest request) {
            return Mono.just(new AiResponse(request.sessionId(), content, false));
        }
    }
}
