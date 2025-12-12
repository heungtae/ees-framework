package com.ees.cluster.membership;

import java.time.Duration;
import java.util.Objects;

/**
 * 멤버십 하트비트 관련 설정 값.
 *
 * @param heartbeatInterval 하트비트 주기
 * @param heartbeatTimeout 타임아웃(이 시간 이상 하트비트 없으면 상태 변경)
 */
public class ClusterMembershipProperties {

    private final Duration heartbeatInterval;
    private final Duration heartbeatTimeout;

    /**
     * 하트비트 주기/타임아웃을 지정해 생성한다.
     */
    public ClusterMembershipProperties(Duration heartbeatInterval, Duration heartbeatTimeout) {
        this.heartbeatInterval = Objects.requireNonNull(heartbeatInterval, "heartbeatInterval must not be null");
        this.heartbeatTimeout = Objects.requireNonNull(heartbeatTimeout, "heartbeatTimeout must not be null");
    }

    /**
     * 기본값(5s/15s)을 반환한다.
     */
    public static ClusterMembershipProperties defaults() {
        return new ClusterMembershipProperties(Duration.ofSeconds(5), Duration.ofSeconds(15));
    }

    /**
     * 하트비트 주기를 반환한다.
     */
    public Duration heartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * 하트비트 타임아웃을 반환한다.
     */
    public Duration heartbeatTimeout() {
        return heartbeatTimeout;
    }
}
