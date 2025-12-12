package com.ees.framework.core;

/**
 * Handler 체인 및 워크플로 정의에서 사용되는 실행 모드.
 * <p>
 * 주로 SourceHandler/SinkHandler 체인과 같이 "여러 핸들러를 어떤 방식으로 실행할지"를 표현한다.
 */
public enum ExecutionMode {
    /**
     * 등록된 순서대로 하나씩 실행한다.
     */
    SEQUENTIAL,

    /**
     * 가능한 경우 병렬로 실행한다.
     */
    PARALLEL
}
