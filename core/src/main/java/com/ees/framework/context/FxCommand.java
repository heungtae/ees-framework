package com.ees.framework.context;

import java.util.Objects;

/**
 * 처리 중 전파되는 커맨드(요청) 정보.
 *
 * @param name 커맨드 이름(널 불가)
 * @param version 커맨드 버전(옵션)
 * @param correlationId 요청 상관관계 ID(옵션)
 */
public record FxCommand(
    String name,
    String version,
    String correlationId
) {
    public FxCommand {
        Objects.requireNonNull(name, "name must not be null");
    }
    /**
     * of를 수행한다.
     * @param name 
     * @return 
     */

    public static FxCommand of(String name) {
        return new FxCommand(name, null, null);
    }
}
