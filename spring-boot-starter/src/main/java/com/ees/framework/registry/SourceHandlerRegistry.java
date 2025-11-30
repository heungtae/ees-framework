package com.ees.framework.registry;

import com.ees.framework.handlers.SourceHandler;

/**
 * SourceHandler 레지스트리.
 * 논리적 이름으로 SourceHandler Bean 을 조회한다.
 */
public interface SourceHandlerRegistry {

    SourceHandler<?> getByName(String name);
}
