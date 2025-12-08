package com.ees.ai.mcp;

import com.ees.ai.core.AiTool;
import com.ees.ai.core.AiToolRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Registers common MCP commands as AI tools and delegates execution to an MCP client.
 */
public class McpToolBridge {

    private final McpClient mcpClient;
    private final AiToolRegistry aiToolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpToolBridge(McpClient mcpClient, AiToolRegistry aiToolRegistry) {
        this.mcpClient = Objects.requireNonNull(mcpClient, "mcpClient must not be null");
        this.aiToolRegistry = Objects.requireNonNull(aiToolRegistry, "aiToolRegistry must not be null");
        registerDefaultTools();
    }

    public List<org.springframework.ai.tool.ToolCallback> toolCallbacks() {
        return List.of(
            callback("listNodes", "List cluster nodes.", schemaNone(), args -> mcpClient.listNodes()),
            callback("describeTopology", "Describe cluster topology.", schemaNone(), args -> mcpClient.describeTopology()),
            callback("startWorkflow", "Start a workflow by ID.", startWorkflowSchema(), args ->
                mcpClient.startWorkflow((String) args.get("workflowId"), args)),
            callback("pauseWorkflow", "Pause a workflow execution.", executionSchema(), args ->
                mcpClient.pauseWorkflow((String) args.get("executionId"))),
            callback("resumeWorkflow", "Resume a workflow execution.", executionSchema(), args ->
                mcpClient.resumeWorkflow((String) args.get("executionId"))),
            callback("cancelWorkflow", "Cancel a workflow execution.", executionSchema(), args ->
                mcpClient.cancelWorkflow((String) args.get("executionId"))),
            callback("getWorkflowState", "Fetch workflow state by execution ID.", executionSchema(), args ->
                mcpClient.getWorkflowState((String) args.get("executionId"))),
            callback("assignKey", "Assign a key/partition to an app.", assignKeySchema(), args ->
                mcpClient.assignKey(
                    (String) args.get("group"),
                    String.valueOf(args.getOrDefault("partition", "")),
                    (String) args.get("key"),
                    (String) args.get("appId")
                )),
            callback("lock", "Acquire a distributed lock with TTL.", lockSchema(), args ->
                mcpClient.lock((String) args.get("name"), asLong(args.get("ttlSeconds"), 0))),
            callback("releaseLock", "Release a distributed lock.", releaseLockSchema(), args ->
                mcpClient.releaseLock((String) args.get("name")))
        );
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

    private org.springframework.ai.tool.ToolCallback callback(
        String name,
        String description,
        String inputSchema,
        Function<Map<String, Object>, Mono<String>> executor
    ) {
        org.springframework.ai.tool.definition.ToolDefinition definition =
            org.springframework.ai.tool.definition.DefaultToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();

        return new org.springframework.ai.tool.ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String request) {
                Map<String, Object> args = parseArgs(request);
                return executor.apply(args).blockOptional().orElse("");
            }
        };
    }

    private Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid tool arguments", e);
        }
    }

    private String schemaNone() {
        return """
            {"type":"object","properties":{}}
            """;
    }

    private String executionSchema() {
        return """
            {"type":"object","properties":{"executionId":{"type":"string"}},"required":["executionId"]}
            """;
    }

    private String startWorkflowSchema() {
        return """
            {"type":"object","properties":{"workflowId":{"type":"string"},"params":{"type":"object"}},"required":["workflowId"]}
            """;
    }

    private String assignKeySchema() {
        return """
            {"type":"object","properties":{"group":{"type":"string"},"partition":{"type":"string"},"key":{"type":"string"},"appId":{"type":"string"}},"required":["group","key","appId"]}
            """;
    }

    private String lockSchema() {
        return """
            {"type":"object","properties":{"name":{"type":"string"},"ttlSeconds":{"type":"integer"}},"required":["name"]}
            """;
    }

    private String releaseLockSchema() {
        return """
            {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}
            """;
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
