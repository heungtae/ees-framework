package com.ees.ai.core;

/**
 * AI가 호출할 수 있는 도구(툴) 메타데이터.
 *
 * @param name 도구 이름(널/공백 불가)
 * @param description 도구 설명(널 불가)
 */
public record AiTool(String name, String description) {

    public AiTool {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (description == null) {
            throw new IllegalArgumentException("description must not be null");
        }
    }
}
