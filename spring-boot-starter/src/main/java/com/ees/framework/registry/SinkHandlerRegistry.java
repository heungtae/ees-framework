package com.ees.framework.registry;

import com.ees.framework.handlers.SinkHandler;

/**
 * SinkHandler 레지스트리.
 * 논리적 이름으로 SinkHandler Bean 을 조회한다.
 */
public interface SinkHandlerRegistry {

    SinkHandler<?> getByName(String name);
}
