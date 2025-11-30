package com.ees.framework.workflow.model;

import com.ees.framework.core.ExecutionMode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Handler 체인에 대한 정의.
 */
public final class HandlerChainDefinition {

    private final ExecutionMode mode;
    private final List<String> handlerNames;

    public HandlerChainDefinition(ExecutionMode mode, List<String> handlerNames) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.handlerNames = List.copyOf(handlerNames);
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public List<String> getHandlerNames() {
        return Collections.unmodifiableList(handlerNames);
    }

    @Override
    public String toString() {
        return "HandlerChainDefinition{" +
            "mode=" + mode +
            ", handlerNames=" + handlerNames +
            '}';
    }
}
