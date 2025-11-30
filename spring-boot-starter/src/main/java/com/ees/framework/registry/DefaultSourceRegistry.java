package com.ees.framework.registry;

import com.ees.framework.annotations.EesSource;
import com.ees.framework.source.Source;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @EesSource(type=...) 메타데이터를 이용한 기본 SourceRegistry 구현.
 */
public class DefaultSourceRegistry implements SourceRegistry {

    private final Map<String, Source<?>> byType = new HashMap<>();

    public DefaultSourceRegistry(List<Source<?>> sources) {
        for (Source<?> src : sources) {
            EesSource ann = src.getClass().getAnnotation(EesSource.class);
            if (ann != null && StringUtils.hasText(ann.type())) {
                byType.put(ann.type(), src);
            }
        }
    }

    @Override
    public Source<?> getByType(String type) {
        Source<?> src = byType.get(type);
        if (src == null) {
            throw new IllegalArgumentException("No Source found for type: " + type);
        }
        return src;
    }
}
