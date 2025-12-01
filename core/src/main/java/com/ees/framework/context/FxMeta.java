package com.ees.framework.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 처리 과정에서 사용하는 메타데이터.
 */
public record FxMeta(
    String sourceId,
    String pipelineStep,
    int retries,
    Map<String, Object> attributes
) {
    public FxMeta {
        attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public static FxMeta empty() {
        return new FxMeta(null, null, 0, Collections.emptyMap());
    }
}
