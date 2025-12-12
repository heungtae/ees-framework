package com.ees.framework.context;

import java.util.Objects;

/**
 * Source → Handler → Pipeline → Sink 흐름에서 전달되는 불변 컨텍스트.
 * <p>
 * 프레임워크 내부에서는 이 컨텍스트를 중심으로 헤더/메타데이터를 누적하며,
 * 단계 간 전달은 새로운 인스턴스를 생성(immutable update)하는 방식으로 이뤄진다.
 *
 * @param command 처리 중 전파되는 커맨드 정보(널 불가)
 * @param headers 헤더(널 불가)
 * @param message 메시지(널 불가)
 * @param meta 메타데이터(널 불가)
 * @param affinity per-key 실행/클러스터 라우팅에 사용할 affinity(널이면 {@link FxAffinity#none()}로 대체)
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

    /**
     * 메타데이터를 교체한 새 컨텍스트를 반환한다.
     *
     * @param meta 새 메타데이터(널 불가)
     * @return 갱신된 컨텍스트
     */
    public FxContext<T> withMeta(FxMeta meta) {
        return new FxContext<>(command, headers, message, meta, affinity);
    }

    /**
     * 헤더를 교체한 새 컨텍스트를 반환한다.
     *
     * @param headers 새 헤더(널 불가)
     * @return 갱신된 컨텍스트
     */
    public FxContext<T> withHeaders(FxHeaders headers) {
        return new FxContext<>(command, headers, message, meta, affinity);
    }

    /**
     * affinity를 교체한 새 컨텍스트를 반환한다.
     *
     * @param affinity 새 affinity(널이면 {@link FxAffinity#none()}로 대체)
     * @return 갱신된 컨텍스트
     */
    public FxContext<T> withAffinity(FxAffinity affinity) {
        return new FxContext<>(command, headers, message, meta, affinity);
    }
}
