package com.ees.ai.core;

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
