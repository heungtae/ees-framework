package com.ees.cluster.spring;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.assignment.ClusterAffinityKindMonitor;
import com.ees.cluster.assignment.InMemoryAssignmentService;
import com.ees.cluster.kafka.KafkaAssignmentCoordinator;
import com.ees.cluster.kafka.KafkaLeaderElectionService;
import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.lock.DefaultDistributedLockService;
import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.membership.ClusterMembershipProperties;
import com.ees.cluster.membership.ClusterMembershipService;
import com.ees.cluster.membership.DefaultClusterMembershipService;
import com.ees.cluster.membership.HeartbeatMonitor;
import com.ees.cluster.model.ClusterMode;
import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterRole;
import com.ees.cluster.raft.RaftAssignmentService;
import com.ees.cluster.raft.RaftLeaderElectionService;
import com.ees.cluster.raft.RaftStateMachineMetrics;
import com.ees.cluster.state.ClusterStateRepository;
import com.ees.cluster.state.MetadataStoreClusterStateRepository;
import com.ees.metadatastore.InMemoryMetadataStore;
import com.ees.metadatastore.MetadataStore;
import com.ees.framework.workflow.affinity.AffinityKindChangeHandler;
import com.ees.framework.workflow.engine.BlockingWorkflowEngine;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Set;
import java.util.List;
import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(ClusterProperties.class)
public class ClusterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MetadataStore metadataStore() {
        return new InMemoryMetadataStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterStateRepository clusterStateRepository(MetadataStore metadataStore) {
        return new MetadataStoreClusterStateRepository(metadataStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterMembershipProperties clusterMembershipProperties(ClusterProperties properties) {
        return new ClusterMembershipProperties(properties.getHeartbeatInterval(), properties.getHeartbeatTimeout());
    }

    @Bean
    @ConditionalOnMissingBean
    public ClusterMembershipService clusterMembershipService(ClusterStateRepository repository,
                                                             ClusterMembershipProperties membershipProperties) {
        return new DefaultClusterMembershipService(repository, membershipProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockService distributedLockService(ClusterStateRepository repository) {
        return new DefaultDistributedLockService(repository);
    }

    @Bean
    @ConditionalOnMissingBean(name = "clusterAssignmentService")
    public AssignmentService clusterAssignmentService(ClusterProperties properties, ClusterStateRepository repository) {
        return properties.getMode() == ClusterMode.RAFT
                ? new RaftAssignmentService(repository, properties.getAssignmentTtl())
                : new InMemoryAssignmentService();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultAffinityKeyExtractor<Object> defaultAffinityKeyExtractor(ClusterProperties properties) {
        return new DefaultAffinityKeyExtractor<>(properties.getAssignmentAffinityKind(),
                value -> value == null ? null : value.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaAssignmentCoordinator kafkaAssignmentCoordinator(AssignmentService assignmentService) {
        return new KafkaAssignmentCoordinator(assignmentService);
    }

    @Bean
    @ConditionalOnBean({BlockingWorkflowEngine.class, WorkflowRuntime.class})
    @ConditionalOnMissingBean
    public ClusterAffinityKindMonitor clusterAffinityKindMonitor(AssignmentService assignmentService,
                                                                 BlockingWorkflowEngine workflowEngine,
                                                                 WorkflowRuntime workflowRuntime) {
        AffinityKindChangeHandler handler = AffinityKindChangeHandler.forRuntime(workflowEngine, workflowRuntime);
        return new ClusterAffinityKindMonitor(assignmentService, handler::onAffinityKindChanged);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public LeaderElectionService leaderElectionService(ClusterProperties properties, ClusterStateRepository repository) {
        return switch (properties.getMode()) {
            case RAFT -> new RaftLeaderElectionService(repository);
            case KAFKA -> new KafkaLeaderElectionService();
        };
    }

    @Bean
    public ClusterNode clusterNode(ClusterProperties properties) {
        Set<ClusterRole> roles = properties.getRoles();
        return new ClusterNode(properties.getNodeId(), properties.getHost(), properties.getPort(),
                roles, properties.getZone(), java.util.Map.of(), "0.0.1");
    }

    @Bean
    @ConditionalOnProperty(prefix = "ees.cluster", name = "heartbeat-enabled", havingValue = "true", matchIfMissing = true)
    public HeartbeatMonitor heartbeatMonitor(ClusterMembershipService membershipService,
                                             ClusterMembershipProperties properties,
                                             ClusterNode clusterNode) {
        return new HeartbeatMonitor(membershipService, properties, clusterNode);
    }

    @Bean
    public HealthIndicator clusterHealthIndicator(ClusterMembershipService membershipService,
                                                  LeaderElectionService leaderElectionService,
                                                  ClusterProperties properties) {
        return () -> {
            boolean leaderPresent = leaderElectionService.getLeader(properties.getLeaderGroup())
                .map(info -> info.leaderNodeId().equals(properties.getNodeId()))
                .orElse(false);
            int members = membershipService.view().size();
            Health.Builder builder = leaderPresent ? Health.up() : Health.unknown();
            builder.withDetail("members", members);
            builder.withDetail("mode", properties.getMode());
            builder.withDetail("leader", leaderPresent);
            return builder.build();
        };
    }

    @Bean
    public ClusterMetricsRegistrar clusterMetricsRegistrar(ClusterMembershipService membershipService,
                                                           MeterRegistry meterRegistry) {
        return new ClusterMetricsRegistrar(membershipService, meterRegistry);
    }

    @Bean
    @ConditionalOnBean(RaftStateMachineMetrics.class)
    public RaftMetricsRegistrar raftMetricsRegistrar(ObjectProvider<List<RaftStateMachineMetrics>> metricsProvider) {
        return new RaftMetricsRegistrar(metricsProvider.getIfAvailable(List::of));
    }

    @Bean
    @ConditionalOnBean(RaftStateMachineMetrics.class)
    public HealthIndicator raftStateMachineHealthIndicator(ObjectProvider<List<RaftStateMachineMetrics>> metricsProvider) {
        return new RaftStateMachineHealthIndicator(metricsProvider.getIfAvailable(List::of), Clock.systemUTC());
    }

    /**
     * Auto-start heartbeat monitor after context is ready.
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean(name = "clusterHeartbeatLifecycle")
    @ConditionalOnProperty(prefix = "ees.cluster", name = "heartbeat-enabled", havingValue = "true", matchIfMissing = true)
    public HeartbeatMonitor clusterHeartbeatLifecycle(HeartbeatMonitor heartbeatMonitor) {
        return heartbeatMonitor;
    }
}
