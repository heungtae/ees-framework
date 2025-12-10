package com.ees.ai.mcp;

import com.ees.ai.core.AiToolRegistry;
import com.ees.ai.core.DefaultAiToolRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class McpToolBridgeTest {

    @Test
    void callbackShouldInvokeMcpClientAndAudit() {
        RecordingAudit audit = new RecordingAudit();
        StubMcpClient client = new StubMcpClient();
        AiToolRegistry registry = new DefaultAiToolRegistry();

        McpToolBridge bridge = new McpToolBridge(client, registry, audit);

        String input = "{\"workflowId\":\"wf-1\"}";
        String result = bridge.toolCallbacks().stream()
            .filter(cb -> cb.getToolDefinition().name().equals("startWorkflow"))
            .findFirst()
            .orElseThrow()
            .call(input);

        Assertions.assertThat(result).isEqualTo("started-wf-1");
        Assertions.assertThat(client.called).containsEntry("startWorkflow", "wf-1");
        Assertions.assertThat(audit.records).isNotEmpty();
        Assertions.assertThat(audit.records.get(0).toolName).isEqualTo("startWorkflow");
    }

    private static class StubMcpClient implements McpClient {

        Map<String, String> called = new HashMap<>();

        @Override
        public String listNodes() {
            return "nodes";
        }

        @Override
        public String describeTopology() {
            return "topology";
        }

        @Override
        public String startWorkflow(String workflowId, Map<String, Object> params) {
            called.put("startWorkflow", workflowId);
            return "started-" + workflowId;
        }

        @Override
        public String pauseWorkflow(String executionId) {
            return "";
        }

        @Override
        public String resumeWorkflow(String executionId) {
            return "";
        }

        @Override
        public String cancelWorkflow(String executionId) {
            return "";
        }

        @Override
        public String getWorkflowState(String executionId) {
            return "";
        }

        @Override
        public String assignKey(String group, String partition, String kind, String key, String appId) {
            return "";
        }

        @Override
        public String lock(String name, long ttl) {
            return "";
        }

        @Override
        public String releaseLock(String name) {
            return "";
        }
    }

    private static class RecordingAudit implements McpAuditService {

        List<Record> records = new ArrayList<>();

        @Override
        public void record(String toolName, Map<String, Object> args, String result, Throwable error) {
            records.add(new Record(toolName, args, result, error));
        }
    }

    private record Record(String toolName, Map<String, Object> args, String result, Throwable error) {
    }
}
