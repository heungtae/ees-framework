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

/**
 * 워크플로 파이프라인에서 AI 에이전트를 호출하는 {@link PipelineStep}.
 * <p>
 * 입력 메시지 payload를 user 메시지로 사용하고, 메타데이터에 주입된 system 프롬프트({@code aiPrompt})와
 * 허용 툴 목록({@code aiToolsAllowed})을 참고하여 {@link AiAgentService}를 호출한다.
 * 결과/오류는 {@link FxMeta#attributes()}에 저장한다.
 */
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

    /**
     * AI 에이전트 서비스로 파이프라인 스텝을 생성한다.
     *
     * @param aiAgentService 호출할 AI 에이전트 서비스
     */
    public AiAgentPipelineStep(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    /**
     * 컨텍스트를 기반으로 AI 요청을 만들고 응답을 메타데이터에 저장한다.
     *
     * @param context 입력 컨텍스트
     * @return AI 응답/오류가 메타에 포함된 컨텍스트
     */
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
    // buildRequest 동작을 수행한다.

    private AiRequest buildRequest(FxContext<Object> context) {
        // FxMeta에 저장된 프롬프트/툴 허용 목록을 읽어 AiRequest를 구성한다.
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
    // attachResponse 동작을 수행한다.

    private FxContext<Object> attachResponse(FxContext<Object> context, com.ees.ai.core.AiResponse response) {
        // aiResponse/aiSessionId를 메타데이터에 저장하고 pipelineStep 정보를 갱신한다.
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
    // attachError 동작을 수행한다.

    private FxContext<Object> attachError(FxContext<Object> context, Throwable ex) {
        // 오류 메시지를 기록하고 retries를 1 증가시킨다.
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
    // extractToolsAllowed 동작을 수행한다.

    private List<String> extractToolsAllowed(Map<String, Object> attributes) {
        // List 또는 CSV 문자열 형태의 aiToolsAllowed를 정규화한다.
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
    // attributeAsString 동작을 수행한다.

    private String attributeAsString(Map<String, Object> attributes, String key) {
        // 메타데이터에서 문자열 값을 안전하게 읽는다.
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
}
