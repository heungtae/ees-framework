package com.ees.framework.registry;

import com.ees.framework.sink.Sink;

/**
 * Sink 타입 레지스트리.
 * 논리적 type (예: "db-order") 로 Sink Bean 을 조회한다.
 */
public interface SinkRegistry {

    Sink<?> getByType(String type);
}
