package com.ees.framework.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 메시지 헤더 모음.
 */
public record FxHeaders(Map<String, String> values) {
    public FxHeaders {
        Objects.requireNonNull(values, "values must not be null");
    }

    public static FxHeaders empty() {
        return new FxHeaders(Collections.emptyMap());
    }

    public String get(String key) {
        return values.get(key);
    }

    public FxHeaders with(String key, String value) {
        Map<String, String> copy = new HashMap<>(values);
        copy.put(key, value);
        return new FxHeaders(Collections.unmodifiableMap(copy));
    }
}
