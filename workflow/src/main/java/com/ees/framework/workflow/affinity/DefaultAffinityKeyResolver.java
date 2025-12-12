package com.ees.framework.workflow.affinity;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxContext;

import java.util.Map;
import java.util.Objects;

/**
 * FxContext.affinity → 헤더/메타 → 기본 kind 순서로 affinity 를 해석하는 기본 구현.
 * 기본 kind 는 클러스터 토폴로지 변경 등에 맞춰 동적으로 교체할 수 있다.
 */
public class DefaultAffinityKeyResolver implements AffinityKeyResolver {

    private volatile String defaultKind;

    /**
     * 기본 affinity kind 를 equipmentId 로 설정한 해석기를 생성한다.
     */
    public DefaultAffinityKeyResolver() {
        this("equipmentId");
    }

    /**
     * 주어진 기본 affinity kind 를 사용하는 해석기를 생성한다.
     *
     * @param defaultKind 컨텍스트에 kind 가 없을 때 사용할 기본 kind
     */
    public DefaultAffinityKeyResolver(String defaultKind) {
        this.defaultKind = defaultKind;
    }

    /**
     * 컨텍스트에서 affinity 를 해석한다.
     * 1) 컨텍스트에 이미 설정된 affinity 가 있으면 그대로 사용
     * 2) 없으면 헤더/메타의 affinity-kind, affinity-value 를 읽어 kind/value 생성
     * 3) value 가 없으면 기본 kind 와 null 값을 반환해 누락을 알린다.
     *
     * @param context affinity 를 읽어올 대상 컨텍스트
     * @return kind/value 를 포함한 affinity (value 가 없으면 value=null)
     */
    @Override
    public FxAffinity resolve(FxContext<?> context) {
        Objects.requireNonNull(context, "context must not be null");
        if (!context.affinity().isEmpty()) {
            return context.affinity();
        }
        String kind = headerOrMeta(context, "affinity-kind");
        String value = headerOrMeta(context, "affinity-value");
        if (value != null) {
            return FxAffinity.of(kind != null ? kind : defaultKind, value);
        }
        // No explicit value; fallback to default kind with empty value to signal missing key.
        return FxAffinity.of(defaultKind, null);
    }

    /**
     * 기본 affinity kind 를 변경한다.
     *
     * @param defaultKind 새 기본 kind
     */
    public void setDefaultKind(String defaultKind) {
        this.defaultKind = defaultKind;
    }

    /**
     * 헤더/컨텍스트에 명시적 kind 가 없을 때 사용할 기본 affinity kind.
     */
    public String defaultKind() {
        return defaultKind;
    }

    private String headerOrMeta(FxContext<?> context, String key) {
        String fromHeaders = context.headers() != null ? context.headers().get(key) : null;
        if (fromHeaders != null) {
            return fromHeaders;
        }
        Map<String, Object> attrs = context.meta() != null ? context.meta().attributes() : Map.of();
        Object val = attrs.get(key);
        return val != null ? val.toString() : null;
    }
}
