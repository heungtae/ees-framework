package com.ees.framework.workflow.model;

import com.ees.framework.core.ExecutionMode;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * Handler 체인에 대한 정의.
 */
@Value
public class HandlerChainDefinition {

    /**
     * 핸들러 실행 모드 (예: SEQUENTIAL, PARALLEL).
     */
    ExecutionMode mode;

    /**
     * 등록할 핸들러 이름 목록 (순서 포함).
     */
    List<String> handlerNames;

    @Builder
    public HandlerChainDefinition(ExecutionMode mode, @Singular List<String> handlerNames) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.handlerNames = List.copyOf(handlerNames);
    }
}
