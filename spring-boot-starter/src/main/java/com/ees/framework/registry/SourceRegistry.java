package com.ees.framework.registry;

import com.ees.framework.source.Source;

/**
 * Source 타입 레지스트리.
 * 논리적 type (예: "kafka-order") 로 Source Bean 을 조회한다.
 */
public interface SourceRegistry {

    Source<?> getByType(String type);
}
