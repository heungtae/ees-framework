package com.ees.framework.workflow.model;

import com.ees.framework.core.ExecutionMode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Handler 체인에 대한 정의.
 */
public final class HandlerChainDefinition {

    private final ExecutionMode mode;       // 핸들러 실행 모드 (예: SEQUENTIAL, PARALLEL)
    private final List<String> handlerNames; // 등록할 핸들러 이름 목록 (순서 포함)

    /**
     * Handler 체인 정의를 생성한다.
     *
     * @param mode         실행 모드
     * @param handlerNames 체인에 포함될 핸들러 이름 목록
     */
    public HandlerChainDefinition(ExecutionMode mode, List<String> handlerNames) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.handlerNames = List.copyOf(handlerNames);
    }

    /** 실행 모드 반환 */
    public ExecutionMode getMode() {
        return mode;
    }

    /** 핸들러 이름 목록 반환 (순서 보존) */
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
