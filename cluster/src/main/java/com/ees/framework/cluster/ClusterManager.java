package com.ees.framework.cluster;

/**
 * 클러스터를 제어하는 기본 인터페이스.
 */
public interface ClusterManager {

    ClusterMode mode();

    void start();

    void shutdown();
}
