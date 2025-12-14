package com.ees.framework.control.config;

import com.ees.ai.control.ControlFacade;
import com.ees.ai.control.ControlProperties;
import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.membership.ClusterMembershipService;
import com.ees.cluster.spring.ClusterProperties;
import com.ees.framework.control.ControlController;
import com.ees.framework.control.ControlTokenAuthFilter;
import com.ees.framework.control.DefaultControlFacade;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Control API(`/api/control/**`) 노출 및 인증 필터를 구성하는 AutoConfiguration.
 */
@AutoConfiguration
@EnableConfigurationProperties(ControlProperties.class)
public class ControlWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ees.control", name = "mode", havingValue = "LOCAL", matchIfMissing = true)
    public ControlFacade controlFacade(ClusterMembershipService membershipService,
                                       LeaderElectionService leaderElectionService,
                                       AssignmentService assignmentService,
                                       DistributedLockService distributedLockService,
                                       WorkflowRuntime workflowRuntime,
                                       ClusterProperties clusterProperties) {
        return new DefaultControlFacade(
            membershipService,
            leaderElectionService,
            assignmentService,
            distributedLockService,
            workflowRuntime,
            clusterProperties
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "ees.control.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ControlController controlController(ControlFacade facade) {
        return new ControlController(facade);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ees.control.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<ControlTokenAuthFilter> controlTokenAuthFilter(ControlProperties properties) {
        FilterRegistrationBean<ControlTokenAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ControlTokenAuthFilter(properties));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
