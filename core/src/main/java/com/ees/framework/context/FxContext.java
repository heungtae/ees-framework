package com.ees.framework.context;

import java.util.Objects;

/**
 * Source → Handler → Pipeline → Sink 흐름에서 전달되는 컨텍스트.
 */
public record FxContext<T>(
    FxCommand command,
    FxHeaders headers,
    FxMessage<T> message,
    FxMeta meta,
    FxAffinity affinity
) {
    public FxContext {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(meta, "meta must not be null");
        affinity = affinity == null ? FxAffinity.none() : affinity;
    }

    public static <T> FxContext<T> of(FxMessage<T> message, FxCommand command) {
        return new FxContext<>(command, FxHeaders.empty(), message, FxMeta.empty(), FxAffinity.none());
    }

    public FxContext<T> withMeta(FxMeta meta) {
        return new FxContext<>(command, headers, message, meta, affinity);
    }

    public FxContext<T> withHeaders(FxHeaders headers) {
        return new FxContext<>(command, headers, message, meta, affinity);
    }

    public FxContext<T> withAffinity(FxAffinity affinity) {
        return new FxContext<>(command, headers, message, meta, affinity);
    }
}
