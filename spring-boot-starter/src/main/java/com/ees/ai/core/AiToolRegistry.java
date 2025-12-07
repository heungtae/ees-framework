package com.ees.ai.core;

import java.util.List;
import java.util.Optional;

public interface AiToolRegistry {

    void register(AiTool tool);

    Optional<AiTool> find(String name);

    List<AiTool> list();
}
