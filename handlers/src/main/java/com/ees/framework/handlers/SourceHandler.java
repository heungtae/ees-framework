package com.ees.framework.handlers;

import com.ees.framework.context.FxContext;
import com.ees.framework.context.SupportsContext;
/**
 * Source 에서 읽어온 컨텍스트에 대해 전/후처리를 수행하는 핸들러.
 *
 * @param <T> 처리 대상 레코드 타입
 */
public interface SourceHandler<T> extends SupportsContext {

    /**
     * 단일 컨텍스트에 대한 처리.
     */
    FxContext<T> handle(FxContext<T> context);
}
