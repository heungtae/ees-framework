package com.ees.framework.context;

import java.util.Objects;

/**
 * 처리 중 전파되는 커맨드 정보.
 */
public record FxCommand(
    String name,
    String version,
    String correlationId
) {
    public FxCommand {
        Objects.requireNonNull(name, "name must not be null");
    }

    public static FxCommand of(String name) {
        return new FxCommand(name, null, null);
    }
}
