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

    @Override
    public AiResponse chat(AiRequest request) {
        String prompt = request.messages().stream()
            .filter(m -> "system".equalsIgnoreCase(m.role()))
            .map(AiMessage::content)
            .findFirst()
            .orElse("system prompt missing");
        String user = request.messages().stream()
            .filter(m -> "user".equalsIgnoreCase(m.role()))
            .map(AiMessage::content)
            .findFirst()
            .orElse("");
        String content = "SUMMARY: " + user.toUpperCase() + " | ACTION: follow-up needed | PROMPT: " + prompt;
        return new AiResponse(request.sessionId(), content, false);
    }
}
