package com.ees.framework.control;

import com.ees.ai.control.ControlFacade;
import com.ees.ai.control.ControlFacade.ControlAssignKeyRequest;
import com.ees.ai.control.ControlFacade.ControlLockRequest;
import com.ees.ai.control.ControlFacade.ControlTopology;
import com.ees.ai.control.ControlFacade.ControlWorkflowState;
import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.leader.LeaderElectionService;
import com.ees.cluster.lock.DistributedLockService;
import com.ees.cluster.membership.ClusterMembershipService;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.KeyAssignmentSource;
import com.ees.cluster.model.LockRecord;
import com.ees.cluster.spring.ClusterProperties;
import com.ees.framework.workflow.engine.Workflow;
import com.ees.framework.workflow.engine.WorkflowRuntime;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.StringUtils;

/**
 * EES 내부 서비스들을 조합해 Control 기능을 제공하는 기본 구현.
 * <p>
 * MVP 단계에서는 “워크플로 실행 인스턴스”가 아닌 “워크플로 런타임 start/stop” 중심으로 제어한다(옵션 A).
 */
public class DefaultControlFacade implements ControlFacade {

    private final ClusterMembershipService membershipService;
    private final LeaderElectionService leaderElectionService;
    private final AssignmentService assignmentService;
    private final DistributedLockService lockService;
    private final WorkflowRuntime workflowRuntime;
    private final ClusterProperties clusterProperties;

    private final ConcurrentHashMap<String, Boolean> workflowRunning = new ConcurrentHashMap<>();

    public DefaultControlFacade(ClusterMembershipService membershipService,
                               LeaderElectionService leaderElectionService,
                               AssignmentService assignmentService,
                               DistributedLockService lockService,
                               WorkflowRuntime workflowRuntime,
                               ClusterProperties clusterProperties) {
        this.membershipService = Objects.requireNonNull(membershipService, "membershipService must not be null");
        this.leaderElectionService = Objects.requireNonNull(leaderElectionService, "leaderElectionService must not be null");
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
        this.lockService = Objects.requireNonNull(lockService, "lockService must not be null");
        this.workflowRuntime = Objects.requireNonNull(workflowRuntime, "workflowRuntime must not be null");
        this.clusterProperties = Objects.requireNonNull(clusterProperties, "clusterProperties must not be null");
    }

    @Override
    public Map<String, ClusterNodeRecord> nodes() {
        return membershipService.view();
    }

    @Override
    public ControlTopology topology() {
        return new ControlTopology(membershipService.view(), leaderElectionService.getLeader(clusterProperties.getLeaderGroup()));
    }

    @Override
    public KeyAssignment assignKey(ControlAssignKeyRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String group = required(request.group(), "group");
        String key = required(request.key(), "key");
        String appId = required(request.appId(), "appId");
        String kind = StringUtils.hasText(request.kind()) ? request.kind().trim() : AssignmentService.DEFAULT_AFFINITY_KIND;
        int partition = request.partition();

        return assignmentService.assignKey(group, partition, kind, key, appId, KeyAssignmentSource.MANUAL);
    }

    @Override
    public Optional<LockRecord> lock(ControlLockRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String name = required(request.name(), "name");
        Duration lease = request.ttlSeconds() > 0 ? Duration.ofSeconds(request.ttlSeconds()) : clusterProperties.getLockLease();
        return lockService.tryAcquire(name, ownerNodeId(), lease, Map.of("source", "control-api"));
    }

    @Override
    public boolean releaseLock(String name) {
        String resolved = required(name, "name");
        return lockService.release(resolved, ownerNodeId());
    }

    @Override
    public ControlWorkflowState startWorkflow(String workflowId, Map<String, Object> params) {
        Workflow workflow = workflow(workflowId);
        workflow.start();
        workflowRunning.put(workflowId, true);
        return new ControlWorkflowState(workflowId, true);
    }

    @Override
    public ControlWorkflowState pauseWorkflow(String workflowId) {
        Workflow workflow = workflow(workflowId);
        workflow.stop();
        workflowRunning.put(workflowId, false);
        return new ControlWorkflowState(workflowId, false);
    }

    @Override
    public ControlWorkflowState resumeWorkflow(String workflowId) {
        return startWorkflow(workflowId, Map.of());
    }

    @Override
    public ControlWorkflowState cancelWorkflow(String workflowId) {
        return pauseWorkflow(workflowId);
    }

    @Override
    public ControlWorkflowState workflowState(String workflowId) {
        required(workflowId, "workflowId");
        boolean running = workflowRunning.getOrDefault(workflowId, false);
        return new ControlWorkflowState(workflowId, running);
    }

    private Workflow workflow(String workflowId) {
        String resolved = required(workflowId, "workflowId");
        return workflowRuntime.getWorkflow(resolved)
            .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + resolved));
    }

    private String ownerNodeId() {
        return "control:" + clusterProperties.getNodeId();
    }

    private String required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

