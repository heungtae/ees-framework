package com.ees.framework.registry;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.sink.Sink;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @FxSink(value=...) 메타데이터를 이용한 기본 SinkRegistry 구현.
 */
public class DefaultSinkRegistry implements SinkRegistry {

    private final Map<String, Sink<?>> byType = new HashMap<>();
    /**
     * 인스턴스를 생성한다.
     * @param sinks 
     */

    public DefaultSinkRegistry(List<Sink<?>> sinks) {
        for (Sink<?> sink : sinks) {
            FxSink ann = sink.getClass().getAnnotation(FxSink.class);
            if (ann != null && StringUtils.hasText(ann.value())) {
                byType.put(ann.value(), sink);
            }
        }
    }
    /**
     * byType를 반환한다.
     * @param type 
     * @return 
     */

    @Override
    public Sink<?> getByType(String type) {
        Sink<?> sink = byType.get(type);
        if (sink == null) {
            throw new IllegalArgumentException("No Sink found for type: " + type);
        }
        return sink;
    }
}
