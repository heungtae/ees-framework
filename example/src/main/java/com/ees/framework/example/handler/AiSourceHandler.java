package com.ees.framework.example.handler;

import com.ees.framework.annotations.SourceHandlerComponent;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMeta;
import com.ees.framework.handlers.SourceHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds AI 분류 프롬프트를 메타데이터에 주입하는 SourceHandler.
 */
@SourceHandlerComponent("ai-source-handler")
public class AiSourceHandler implements SourceHandler<String> {

    private static final String AI_PROMPT_KEY = "aiPrompt";
    private static final String CLASSIFICATION_PROMPT =
        "너는 수신된 문장을 GREETING, QUESTION, ALERT 중 하나로 분류하고 한 줄 이유를 제시한다. "
            + "응답 형식: CLASSIFICATION: <LABEL>; REASON: <text>";

    @Override
    public FxContext<String> handle(FxContext<String> context) {
        Map<String, Object> attributes = new HashMap<>(context.meta().attributes());
        attributes.put(AI_PROMPT_KEY, CLASSIFICATION_PROMPT);
        FxMeta meta = new FxMeta(
            context.meta().sourceId(),
            context.meta().pipelineStep(),
            context.meta().retries(),
            attributes
        );
        return new FxContext<>(context.command(), context.headers(), context.message(), meta, context.affinity());
    }

    @Override
    public boolean supports(FxContext<?> context) {
        return "example-greeting".equals(context.message().sourceType());
    }
}
