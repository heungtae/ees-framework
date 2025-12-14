package com.ees.ai.control;

import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.LeaderInfo;
import com.ees.cluster.model.LockRecord;
import java.util.Map;
import java.util.Optional;

/**
 * EES 내부 제어 기능을 캡슐화하는 공용 서비스 파사드.
 * <p>
 * - Web Controller와 AI Tool이 동일 파사드를 호출하도록 하여 로직/검증/감사를 재사용한다.
 * - Embedded 모드에서는 {@link ControlClient}가 이 파사드를 직접 호출한다.
 */
public interface ControlFacade {

    Map<String, ClusterNodeRecord> nodes();

    ControlTopology topology();

    KeyAssignment assignKey(ControlAssignKeyRequest request);

    Optional<LockRecord> lock(ControlLockRequest request);

    boolean releaseLock(String name);

    ControlWorkflowState startWorkflow(String workflowId, Map<String, Object> params);

    ControlWorkflowState pauseWorkflow(String workflowId);

    ControlWorkflowState resumeWorkflow(String workflowId);

    ControlWorkflowState cancelWorkflow(String workflowId);

    ControlWorkflowState workflowState(String workflowId);

    /**
     * 토폴로지 응답 DTO.
     *
     * @param nodes 멤버십 뷰
     * @param leader 현재 리더(없으면 empty)
     */
    record ControlTopology(Map<String, ClusterNodeRecord> nodes, Optional<LeaderInfo> leader) { }

    /**
     * 키 할당 요청 DTO.
     */
    record ControlAssignKeyRequest(String group, int partition, String kind, String key, String appId) { }

    /**
     * 락 획득 요청 DTO.
     */
    record ControlLockRequest(String name, long ttlSeconds, String owner) { }

    /**
     * 워크플로 상태 DTO(MVP: 런타임 제어형).
     */
    record ControlWorkflowState(String workflowId, boolean running) { }
}

