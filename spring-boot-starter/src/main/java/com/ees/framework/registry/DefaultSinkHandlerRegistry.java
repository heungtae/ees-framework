package com.ees.framework.registry;

import com.ees.framework.annotations.SinkHandlerComponent;
import com.ees.framework.handlers.SinkHandler;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @SinkHandlerComponent(value=...) 메타데이터를 이용한 기본 SinkHandlerRegistry 구현.
 */
public class DefaultSinkHandlerRegistry implements SinkHandlerRegistry {

    private final Map<String, SinkHandler<?>> byName = new HashMap<>();
    /**
     * 인스턴스를 생성한다.
     * @param handlers 
     */

    public DefaultSinkHandlerRegistry(List<SinkHandler<?>> handlers) {
        for (SinkHandler<?> h : handlers) {
            SinkHandlerComponent ann = h.getClass().getAnnotation(SinkHandlerComponent.class);
            if (ann != null && StringUtils.hasText(ann.value())) {
                byName.put(ann.value(), h);
            }
        }
    }
    /**
     * byName를 반환한다.
     * @param name 
     * @return 
     */

    @Override
    public SinkHandler<?> getByName(String name) {
        SinkHandler<?> h = byName.get(name);
        if (h == null) {
            throw new IllegalArgumentException("No SinkHandler found for name: " + name);
        }
        return h;
    }
}
