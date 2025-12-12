package com.ees.cluster.spring;

import com.ees.cluster.membership.ClusterMembershipService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * 클러스터 멤버십 관련 메트릭을 Micrometer에 등록한다.
 */
public class ClusterMetricsRegistrar implements MeterBinder {

    private final ClusterMembershipService membershipService;

    /**
     * 멤버십 서비스로 메트릭 등록기를 생성한다.
     *
     * @param membershipService 멤버십 서비스
     * @param registry 미사용(호환을 위한 파라미터)
     */
    public ClusterMetricsRegistrar(ClusterMembershipService membershipService, MeterRegistry registry) {
        this.membershipService = membershipService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        meterRegistry.gauge("ees.cluster.members", membershipService,
                svc -> svc.view().size());
    }
}
