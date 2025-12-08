package com.ees.ai.workflow;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiMessage;
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

import java.util.List;
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
                    .containsEntry(AiAgentPipelineStep.ATTR_RESPONSE, "decision");
                Assertions.assertThat(result.meta().attributes()).containsEntry(AiAgentPipelineStep.ATTR_SESSION, "workflow-cmd");
                Assertions.assertThat(result.meta().pipelineStep()).isEqualTo(AiAgentPipelineStep.PIPELINE_STEP);
            })
            .verifyComplete();
    }

    @Test
    void shouldPassPromptAndToolsToAiRequest() {
        RecordingAiAgentService recording = new RecordingAiAgentService();
        AiAgentPipelineStep step = new AiAgentPipelineStep(recording);

        FxContext<Object> context = new FxContext<>(
            FxCommand.of("workflow-cmd"),
            FxHeaders.empty(),
            FxMessage.now("source", "input"),
            new FxMeta("session-1", null, 0, Map.of(
                AiAgentPipelineStep.ATTR_PROMPT, "system guidance",
                AiAgentPipelineStep.ATTR_TOOLS_ALLOWED, List.of("listNodes", "describeTopology")
            ))
        );

        StepVerifier.create(step.apply(context))
            .expectNextCount(1)
            .verifyComplete();

        Assertions.assertThat(recording.lastRequest).isNotNull();
        Assertions.assertThat(recording.lastRequest.sessionId()).isEqualTo("session-1");
        Assertions.assertThat(recording.lastRequest.toolsAllowed()).containsExactly("listNodes", "describeTopology");
        Assertions.assertThat(recording.lastRequest.messages())
            .extracting(AiMessage::role)
            .containsExactly("system", "user");
        Assertions.assertThat(recording.lastRequest.messages().get(0).content()).isEqualTo("system guidance");
        Assertions.assertThat(recording.lastRequest.messages().get(1).content()).isEqualTo("input");
    }

    @Test
    void shouldAttachErrorWhenAiCallFails() {
        AiAgentService failing = request -> Mono.error(new IllegalStateException("ai unavailable"));
        AiAgentPipelineStep step = new AiAgentPipelineStep(failing);

        FxContext<Object> context = new FxContext<>(
            FxCommand.of("workflow-cmd"),
            FxHeaders.empty(),
            FxMessage.now("source", "input"),
            FxMeta.empty()
        );

        StepVerifier.create(step.apply(context))
            .assertNext(result -> {
                Assertions.assertThat(result.meta().attributes())
                    .containsEntry(AiAgentPipelineStep.ATTR_ERROR, "ai unavailable");
                Assertions.assertThat(result.meta().pipelineStep()).isEqualTo(AiAgentPipelineStep.PIPELINE_STEP);
                Assertions.assertThat(result.meta().retries()).isEqualTo(1);
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

    private static class RecordingAiAgentService implements AiAgentService {

        private AiRequest lastRequest;

        @Override
        public Mono<AiResponse> chat(AiRequest request) {
            this.lastRequest = request;
            return Mono.just(new AiResponse(request.sessionId(), "ok", false));
        }
    }
}
