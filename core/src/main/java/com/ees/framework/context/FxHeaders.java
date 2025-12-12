package com.ees.framework.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 메시지 헤더 모음.
 * <p>
 * 내부적으로 불변 맵을 유지하며, {@link #with(String, String)} 호출 시 복사본을 생성한다.
 *
 * @param values 헤더 키-값(널 불가)
 */
public record FxHeaders(Map<String, String> values) {
    public FxHeaders {
        Objects.requireNonNull(values, "values must not be null");
    }

    /**
     * 빈 헤더를 반환한다.
     *
     * @return 빈 헤더
     */
    public static FxHeaders empty() {
        return new FxHeaders(Collections.emptyMap());
    }

    /**
     * 헤더 값을 조회한다.
     *
     * @param key 헤더 키
     * @return 헤더 값(없으면 null)
     */
    public String get(String key) {
        return values.get(key);
    }

    /**
     * 헤더를 추가/덮어쓴 새 헤더 인스턴스를 반환한다.
     *
     * @param key 헤더 키
     * @param value 헤더 값
     * @return 갱신된 헤더
     */
    public FxHeaders with(String key, String value) {
        Map<String, String> copy = new HashMap<>(values);
        copy.put(key, value);
        return new FxHeaders(Collections.unmodifiableMap(copy));
    }
}
