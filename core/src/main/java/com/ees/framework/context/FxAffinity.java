package com.ees.framework.context;

/**
 * Affinity key(kind + value) used for clustering and per-key execution.
 */
public record FxAffinity(String kind, String value) {

    public static FxAffinity of(String kind, String value) {
        if (kind == null && value == null) {
            return none();
        }
        return new FxAffinity(kind, value);
    }

    public static FxAffinity none() {
        return new FxAffinity(null, null);
    }

    public boolean isEmpty() {
        return kind == null || value == null;
    }
}
