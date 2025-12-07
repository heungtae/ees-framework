package com.ees.cluster.model;

import java.util.Map;
import java.util.Objects;

public record WorkflowHandoff(
        String checkpoint,
        Map<String, String> metadata
) {

    public WorkflowHandoff {
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
    }
}
