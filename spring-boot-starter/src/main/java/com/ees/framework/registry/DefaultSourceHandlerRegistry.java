package com.ees.framework.registry;

import com.ees.framework.annotations.SourceHandlerComponent;
import com.ees.framework.handlers.SourceHandler;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @SourceHandlerComponent(value=...) 메타데이터를 이용한 기본 SourceHandlerRegistry 구현.
 */
public class DefaultSourceHandlerRegistry implements SourceHandlerRegistry {

    private final Map<String, SourceHandler<?>> byName = new HashMap<>();

    public DefaultSourceHandlerRegistry(List<SourceHandler<?>> handlers) {
        for (SourceHandler<?> h : handlers) {
            SourceHandlerComponent ann = h.getClass().getAnnotation(SourceHandlerComponent.class);
            if (ann != null && StringUtils.hasText(ann.value())) {
                byName.put(ann.value(), h);
            }
        }
    }

    @Override
    public SourceHandler<?> getByName(String name) {
        SourceHandler<?> h = byName.get(name);
        if (h == null) {
            throw new IllegalArgumentException("No SourceHandler found for name: " + name);
        }
        return h;
    }
}
