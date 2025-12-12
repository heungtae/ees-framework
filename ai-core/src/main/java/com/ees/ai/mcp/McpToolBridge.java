package com.ees.ai.mcp;

import com.ees.ai.core.AiTool;
import com.ees.ai.core.AiToolRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ees.cluster.assignment.AssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Registers common MCP commands as AI tools and delegates execution to an MCP client.
 */
public class McpToolBridge {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(McpToolBridge.class);

    private final McpClient mcpClient;
    private final AiToolRegistry aiToolRegistry;
    // ObjectMapper 동작을 수행한다.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final McpAuditService auditService;
    /**
     * 인스턴스를 생성한다.
     * @param mcpClient 
     * @param aiToolRegistry 
     * @param auditService 
     */

    public McpToolBridge(McpClient mcpClient, AiToolRegistry aiToolRegistry, McpAuditService auditService) {
        this.mcpClient = Objects.requireNonNull(mcpClient, "mcpClient must not be null");
        this.aiToolRegistry = Objects.requireNonNull(aiToolRegistry, "aiToolRegistry must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        registerDefaultTools();
    }
    /**
     * toolCallbacks를 수행한다.
     * @return 
     */

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
                    resolveKind(args),
                    (String) args.get("key"),
                    (String) args.get("appId")
                )),
            callback("lock", "Acquire a distributed lock with TTL.", lockSchema(), args ->
                mcpClient.lock((String) args.get("name"), asLong(args.get("ttlSeconds"), 0))),
            callback("releaseLock", "Release a distributed lock.", releaseLockSchema(), args ->
                mcpClient.releaseLock((String) args.get("name")))
        );
    }
    /**
     * invoke를 수행한다.
     * @param toolName 
     * @param args 
     * @return 
     */

    public String invoke(String toolName, Map<String, Object> args) {
        log.info("Invoking MCP tool: {} args={}", toolName, args);
        try {
            String result = switch (toolName) {
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
                    resolveKind(args),
                    (String) args.getOrDefault("key", ""),
                    (String) args.getOrDefault("appId", "")
                );
                case "lock" ->
                    mcpClient.lock((String) args.getOrDefault("name", ""), asLong(args.get("ttl"), 0));
                case "releaseLock" ->
                    mcpClient.releaseLock((String) args.getOrDefault("name", ""));
                default -> throw new IllegalArgumentException("Unsupported MCP tool: " + toolName);
            };
            auditService.record(toolName, args, result, null);
            return result;
        } catch (Exception ex) {
            auditService.record(toolName, args, null, ex);
            throw ex;
        }
    }
    // asLong 동작을 수행한다.

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
    // callback 동작을 수행한다.

    private org.springframework.ai.tool.ToolCallback callback(
        String name,
        String description,
        String inputSchema,
        Function<Map<String, Object>, String> executor
    ) {
        org.springframework.ai.tool.definition.ToolDefinition definition =
            org.springframework.ai.tool.definition.DefaultToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();

        return new org.springframework.ai.tool.ToolCallback() {
            /**
             * toolDefinition를 반환한다.
             * @return 
             */
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return definition;
            }
            /**
             * call를 수행한다.
             * @param request 
             * @return 
             */

            @Override
            public String call(String request) {
                Map<String, Object> args = parseArgs(request);
                try {
                    String result = executor.apply(args);
                    auditService.record(definition.name(), args, result, null);
                    return result;
                } catch (Exception ex) {
                    auditService.record(definition.name(), args, null, ex);
                    throw ex;
                }
            }
        };
    }
    // parseArgs 동작을 수행한다.

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
    // schemaNone 동작을 수행한다.

    private String schemaNone() {
        return """
            {"type":"object","properties":{}}
            """;
    }
    // executionSchema 동작을 수행한다.

    private String executionSchema() {
        return """
            {"type":"object","properties":{"executionId":{"type":"string"}},"required":["executionId"]}
            """;
    }
    // startWorkflowSchema 동작을 수행한다.

    private String startWorkflowSchema() {
        return """
            {"type":"object","properties":{"workflowId":{"type":"string"},"params":{"type":"object"}},"required":["workflowId"]}
            """;
    }
    // assignKeySchema 동작을 수행한다.

    private String assignKeySchema() {
        return """
            {"type":"object","properties":{"group":{"type":"string"},"partition":{"type":"string"},"kind":{"type":"string"},"key":{"type":"string"},"appId":{"type":"string"}},"required":["group","kind","key","appId"]}
            """;
    }
    // resolveKind 동작을 수행한다.

    private String resolveKind(Map<String, Object> args) {
        Object provided = args.get("kind");
        if (provided instanceof String text && !text.isBlank()) {
            return text;
        }
        return AssignmentService.DEFAULT_AFFINITY_KIND;
    }
    // lockSchema 동작을 수행한다.

    private String lockSchema() {
        return """
            {"type":"object","properties":{"name":{"type":"string"},"ttlSeconds":{"type":"integer"}},"required":["name"]}
            """;
    }
    // releaseLockSchema 동작을 수행한다.

    private String releaseLockSchema() {
        return """
            {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}
            """;
    }
    // registerDefaultTools 동작을 수행한다.

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
