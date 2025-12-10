package com.ees.ai.core;

public interface AiAgentService {

    AiResponse chat(AiRequest request);

    default java.util.List<AiResponse> chatStream(AiRequest request) {
        return java.util.List.of(chat(request));
    }
}
