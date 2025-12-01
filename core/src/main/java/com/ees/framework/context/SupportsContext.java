package com.ees.framework.context;

/**
 * 컨텍스트를 처리할 수 있는지 여부를 판단하는 기능.
 * 기본 구현은 항상 true를 반환한다.
 */
public interface SupportsContext {

    /**
     * 주어진 컨텍스트를 처리할 수 있는지 여부를 반환한다.
     *
     * @param context 처리 대상 컨텍스트
     * @return 처리 가능 여부 (default: true)
     */
    default boolean supports(FxContext<?> context) {
        return true;
    }
}
