package com.ees.cluster.model;

import java.util.Map;
import java.util.Objects;

/**
 * 워크플로 handoff(체크포인트/메타데이터) 정보를 표현한다.
 *
 * @param checkpoint 체크포인트(널 불가)
 * @param metadata 추가 메타데이터(널 불가)
 */
public record WorkflowHandoff(
        String checkpoint,
        Map<String, String> metadata
) {

    public WorkflowHandoff {
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
    }
}
