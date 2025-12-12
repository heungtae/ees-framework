package com.ees.cluster.model;

/**
 * 클러스터 노드의 상태.
 */
public enum ClusterNodeStatus {
    JOINING,
    UP,
    SUSPECT,
    DOWN,
    LEFT
}
