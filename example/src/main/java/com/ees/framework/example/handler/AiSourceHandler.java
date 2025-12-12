package com.ees.framework.example.handler;

import com.ees.framework.annotations.SourceHandlerComponent;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMeta;
import com.ees.framework.handlers.SourceHandler;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adds AI 분류 프롬프트를 메타데이터에 주입하는 SourceHandler.
 * 프롬프트와 지원 소스 타입은 프로퍼티로 조정할 수 있다.
 */
@SourceHandlerComponent("ai-source-handler")
public class AiSourceHandler implements SourceHandler<String> {

    private static final String AI_PROMPT_KEY = "aiPrompt";
    private static final String DEFAULT_PROMPT =
        "너는 수신된 문장을 GREETING, QUESTION, ALERT 중 하나로 분류하고 한 줄 이유를 제시한다. "
            + "응답 형식: CLASSIFICATION: <LABEL>; REASON: <text>";
    private static final String DEFAULT_SOURCES = "example-greeting";

    private final String classificationPrompt;
    private final Set<String> supportedSources;

    public AiSourceHandler(
        @Value("${example.ai.classification-prompt:}") String promptProperty,
        @Value("${example.ai.supported-sources:}") String supportedSourcesProperty
    ) {
        this.classificationPrompt = promptProperty == null || promptProperty.isBlank()
            ? DEFAULT_PROMPT
            : promptProperty;
        String sources = supportedSourcesProperty == null || supportedSourcesProperty.isBlank()
            ? DEFAULT_SOURCES
            : supportedSourcesProperty;
        this.supportedSources = parseSupportedSources(sources);
    }

    @Override
    public FxContext<String> handle(FxContext<String> context) {
        Map<String, Object> attributes = new HashMap<>(context.meta().attributes());
        attributes.put(AI_PROMPT_KEY, classificationPrompt);
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
        return context.message() != null
            && supportedSources.contains(context.message().sourceType());
    }

    private Set<String> parseSupportedSources(String csv) {
        return Stream.of(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());
    }
}
