package com.ees.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAiToolRegistry implements AiToolRegistry {

    private final Map<String, AiTool> tools = new ConcurrentHashMap<>();

    @Override
    public void register(AiTool tool) {
        tools.put(tool.name(), tool);
    }

    @Override
    public Optional<AiTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    @Override
    public List<AiTool> list() {
        return new ArrayList<>(tools.values());
    }
}
