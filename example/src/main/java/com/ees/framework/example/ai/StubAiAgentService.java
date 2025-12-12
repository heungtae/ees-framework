package com.ees.framework.example.ai;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiMessage;
import com.ees.ai.core.AiRequest;
import com.ees.ai.core.AiResponse;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.List;

/**
 * 외부 모델 호출 없이 deterministic 응답을 생성하는 스텁 AI 에이전트 서비스.
 */
@Component
@ConditionalOnMissingBean(AiAgentService.class)
public class StubAiAgentService implements AiAgentService {

    /**
     * 시스템 프롬프트와 사용자 입력을 읽어 분류/요약을 결정하고 deterministic 응답을 생성한다.
     *
     * @param request AI 요청 메시지(시스템/사용자)
     * @return 고정된 형식의 AI 응답
     */
    @Override
    public AiResponse chat(AiRequest request) {
        String prompt = systemPrompt(request);
        String user = userMessage(request);
        String content = prompt.toLowerCase().contains("분류")
            ? classify(user)
            : summarize(user, prompt);
        return new AiResponse(request.sessionId(), content, false);
    }
    // systemPrompt 동작을 수행한다.

    private String systemPrompt(AiRequest request) {
        return request.messages().stream()
            .filter(m -> "system".equalsIgnoreCase(m.role()))
            .map(AiMessage::content)
            .findFirst()
            .orElse("system prompt missing");
    }
    // userMessage 동작을 수행한다.

    private String userMessage(AiRequest request) {
        return request.messages().stream()
            .filter(m -> "user".equalsIgnoreCase(m.role()))
            .map(AiMessage::content)
            .findFirst()
            .orElse("");
    }
    // summarize 동작을 수행한다.

    private String summarize(String user, String prompt) {
        return "SUMMARY: " + user.toUpperCase()
            + " | ACTION: follow-up needed | PROMPT: " + prompt;
    }
    // classify 동작을 수행한다.

    private String classify(String user) {
        String normalized = user.toLowerCase();
        String label;
        if (normalized.contains("error") || normalized.contains("fail")) {
            label = "ALERT";
        } else if (normalized.contains("?")) {
            label = "QUESTION";
        } else {
            label = "GREETING";
        }
        return "CLASSIFICATION: " + label + "; REASON: stub-classifier";
    }
}
