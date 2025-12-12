package com.ees.cluster.model;

/**
 * 클러스터 노드의 역할.
 */
public enum ClusterRole {
    SOURCE,
    HANDLER,
    PIPELINE,
    SINK,
    WORKFLOW
}
