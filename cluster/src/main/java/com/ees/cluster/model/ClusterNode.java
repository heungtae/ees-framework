package com.ees.cluster.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 클러스터 참여 노드를 표현하는 모델.
 *
 * @param nodeId 노드 ID(널 불가)
 * @param host 호스트(널 불가)
 * @param port 포트(1~65535)
 * @param roles 역할 집합(널 불가)
 * @param zone 존/가용영역(옵션)
 * @param metadata 추가 메타데이터(널 불가)
 * @param version 노드 버전(옵션)
 */
public record ClusterNode(
        String nodeId,
        String host,
        int port,
        Set<ClusterRole> roles,
        String zone,
        Map<String, String> metadata,
        String version
) {
    /**
     * 인스턴스를 생성한다.
     * @param nodeId 
     * @param host 
     * @param port 
     * @param roles 
     * @param zone 
     * @param metadata 
     * @param version 
     */

    public ClusterNode(String nodeId,
                       String host,
                       int port,
                       Set<ClusterRole> roles,
                       String zone,
                       Map<String, String> metadata,
                       String version) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.port = validatePort(port);
        this.roles = Collections.unmodifiableSet(Set.copyOf(Objects.requireNonNull(roles, "roles must not be null")));
        this.zone = zone;
        this.metadata = Collections.unmodifiableMap(Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null")));
        this.version = version;
    }
    // validatePort 동작을 수행한다.

    private static int validatePort(int port) {
        // TCP 포트 범위를 검증한다.
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        return port;
    }
}
