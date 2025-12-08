package com.ees.ai.mcp;

import com.ees.ai.core.AiTool;
import com.ees.ai.core.AiToolRegistry;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registers common MCP commands as AI tools and delegates execution to an MCP client.
 */
public class McpToolBridge {

    private final McpClient mcpClient;
    private final AiToolRegistry aiToolRegistry;

    public McpToolBridge(McpClient mcpClient, AiToolRegistry aiToolRegistry) {
        this.mcpClient = Objects.requireNonNull(mcpClient, "mcpClient must not be null");
        this.aiToolRegistry = Objects.requireNonNull(aiToolRegistry, "aiToolRegistry must not be null");
        registerDefaultTools();
    }

    public Mono<String> invoke(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "listNodes" -> mcpClient.listNodes();
            case "describeTopology" -> mcpClient.describeTopology();
            case "startWorkflow" ->
                mcpClient.startWorkflow((String) args.getOrDefault("workflowId", ""), args);
            case "pauseWorkflow" ->
                mcpClient.pauseWorkflow((String) args.getOrDefault("executionId", ""));
            case "resumeWorkflow" ->
                mcpClient.resumeWorkflow((String) args.getOrDefault("executionId", ""));
            case "cancelWorkflow" ->
                mcpClient.cancelWorkflow((String) args.getOrDefault("executionId", ""));
            case "getWorkflowState" ->
                mcpClient.getWorkflowState((String) args.getOrDefault("executionId", ""));
            case "assignKey" -> mcpClient.assignKey(
                (String) args.getOrDefault("group", ""),
                String.valueOf(args.getOrDefault("partition", "")),
                (String) args.getOrDefault("key", ""),
                (String) args.getOrDefault("appId", "")
            );
            case "lock" ->
                mcpClient.lock((String) args.getOrDefault("name", ""), asLong(args.get("ttl"), 0));
            case "releaseLock" ->
                mcpClient.releaseLock((String) args.getOrDefault("name", ""));
            default -> Mono.error(new IllegalArgumentException("Unsupported MCP tool: " + toolName));
        };
    }

    private long asLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void registerDefaultTools() {
        List.of(
            new AiTool("listNodes", "List cluster nodes."),
            new AiTool("describeTopology", "Describe cluster topology."),
            new AiTool("startWorkflow", "Start a workflow by ID."),
            new AiTool("pauseWorkflow", "Pause a workflow execution."),
            new AiTool("resumeWorkflow", "Resume a workflow execution."),
            new AiTool("cancelWorkflow", "Cancel a workflow execution."),
            new AiTool("getWorkflowState", "Fetch workflow state by execution ID."),
            new AiTool("assignKey", "Assign a key/partition to an app."),
            new AiTool("lock", "Acquire a distributed lock with TTL."),
            new AiTool("releaseLock", "Release a distributed lock.")
        ).forEach(aiToolRegistry::register);
    }
}
