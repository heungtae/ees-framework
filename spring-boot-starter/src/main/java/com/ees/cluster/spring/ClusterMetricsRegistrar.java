package com.ees.cluster.spring;

import com.ees.cluster.membership.ClusterMembershipService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

public class ClusterMetricsRegistrar implements MeterBinder {

    private final ClusterMembershipService membershipService;

    public ClusterMetricsRegistrar(ClusterMembershipService membershipService, MeterRegistry registry) {
        this.membershipService = membershipService;
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        meterRegistry.gauge("ees.cluster.members", membershipService,
                svc -> svc.view().map(view -> view.size()).blockOptional().orElse(0));
    }
}
