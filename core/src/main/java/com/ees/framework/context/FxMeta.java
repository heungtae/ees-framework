package com.ees.framework.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 처리 과정에서 사용하는 메타데이터.
 * <p>
 * {@code attributes}는 불변(읽기 전용) 맵으로 보관되며, {@code null}인 경우 빈 맵으로 대체된다.
 *
 * @param sourceId 메시지를 생성한 소스 인스턴스 식별자(옵션)
 * @param pipelineStep 현재(또는 마지막) 파이프라인 스텝 이름(옵션)
 * @param retries 재시도 횟수
 * @param attributes 커스텀 속성(옵션)
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
    /**
     * empty를 수행한다.
     * @return 
     */

    public static FxMeta empty() {
        return new FxMeta(null, null, 0, Collections.emptyMap());
    }
}
