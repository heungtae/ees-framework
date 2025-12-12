package com.ees.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * in-memory 기반 {@link AiToolRegistry} 기본 구현.
 */
public class DefaultAiToolRegistry implements AiToolRegistry {

    private final Map<String, AiTool> tools = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(AiTool tool) {
        tools.put(tool.name(), tool);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AiTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AiTool> list() {
        return new ArrayList<>(tools.values());
    }
}
