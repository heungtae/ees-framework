package com.ees.ai.control;

import java.util.Map;

/**
 * EES Control 기능을 호출하기 위한 클라이언트 파사드.
 * <p>
 * AI Tool 호출 경로에서 사용하기 위해 "문자열(JSON) 결과" 형태를 기본으로 한다.
 */
public interface ControlClient {

    String listNodes();

    String describeTopology();

    String startWorkflow(String workflowId, Map<String, Object> params);

    String pauseWorkflow(String workflowId);

    String resumeWorkflow(String workflowId);

    String cancelWorkflow(String workflowId);

    String getWorkflowState(String workflowId);

    String assignKey(String group, String partition, String kind, String key, String appId);

    String lock(String name, long ttlSeconds);

    String releaseLock(String name);
}

