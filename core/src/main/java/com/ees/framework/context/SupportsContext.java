package com.ees.framework.context;

/**
 * 주어진 {@link FxContext}를 처리할 수 있는지 여부를 판단하는 기능.
 * <p>
 * SourceHandler/PipelineStep/SinkHandler 등에서 선택적으로 구현하여, 특정 sourceType이나
 * 헤더/메타 조건에 따라 적용 여부를 제어할 수 있다.
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
