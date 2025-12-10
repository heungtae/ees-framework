package com.ees.framework.workflow.affinity;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxContext;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves affinity by prioritizing FxContext.affinity, then headers/meta, then an optional default kind.
 */
public class DefaultAffinityKeyResolver implements AffinityKeyResolver {

    private volatile String defaultKind;

    public DefaultAffinityKeyResolver() {
        this("equipmentId");
    }

    public DefaultAffinityKeyResolver(String defaultKind) {
        this.defaultKind = defaultKind;
    }

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

    public void setDefaultKind(String defaultKind) {
        this.defaultKind = defaultKind;
    }

    /**
     * Current default affinity kind used when headers/context do not provide one explicitly.
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
