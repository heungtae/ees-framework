package com.ees.ai.control;

import com.ees.ai.core.AiToolRegistry;
import com.ees.ai.core.AiTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Control 명령을 Spring AI ToolCallback으로 등록하는 브릿지.
 * <p>
 * Tool 이름은 기존 MCP 기반 명령 이름과 동일하게 유지하여 UI/프롬프트 호환성을 확보한다.
 */
public class ControlToolBridge {

    private final ControlClient controlClient;
    private final AiToolRegistry aiToolRegistry;
    private final ObjectMapper objectMapper;
    private final ControlAuditService auditService;

    public ControlToolBridge(ControlClient controlClient,
                             AiToolRegistry aiToolRegistry,
                             ObjectMapper objectMapper,
                             ControlAuditService auditService) {
        this.controlClient = Objects.requireNonNull(controlClient, "controlClient must not be null");
        this.aiToolRegistry = Objects.requireNonNull(aiToolRegistry, "aiToolRegistry must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        registerDefaultTools();
    }

    /**
     * Control 툴 콜백 목록을 반환한다.
     */
    public List<ToolCallback> toolCallbacks() {
        return List.of(
            callback("listNodes", "List cluster nodes.", schemaNone(), args -> controlClient.listNodes()),
            callback("describeTopology", "Describe cluster topology.", schemaNone(), args -> controlClient.describeTopology()),
            callback("startWorkflow", "Start a workflow by ID.", startWorkflowSchema(), args ->
                controlClient.startWorkflow((String) args.get("workflowId"), args)),
            callback("pauseWorkflow", "Pause a workflow by ID (runtime control).", workflowIdSchema(), args ->
                controlClient.pauseWorkflow((String) args.get("workflowId"))),
            callback("resumeWorkflow", "Resume a workflow by ID (runtime control).", workflowIdSchema(), args ->
                controlClient.resumeWorkflow((String) args.get("workflowId"))),
            callback("cancelWorkflow", "Cancel a workflow by ID (runtime control).", workflowIdSchema(), args ->
                controlClient.cancelWorkflow((String) args.get("workflowId"))),
            callback("getWorkflowState", "Fetch workflow state by workflow ID.", workflowIdSchema(), args ->
                controlClient.getWorkflowState((String) args.get("workflowId"))),
            callback("assignKey", "Assign a key/partition to an app.", assignKeySchema(), args ->
                controlClient.assignKey(
                    (String) args.get("group"),
                    String.valueOf(args.getOrDefault("partition", "0")),
                    String.valueOf(args.getOrDefault("kind", "")),
                    (String) args.get("key"),
                    (String) args.get("appId")
                )),
            callback("lock", "Acquire a distributed lock with TTL.", lockSchema(), args ->
                controlClient.lock((String) args.get("name"), asLong(args.get("ttlSeconds"), 0))),
            callback("releaseLock", "Release a distributed lock.", releaseLockSchema(), args ->
                controlClient.releaseLock((String) args.get("name")))
        );
    }

    private void registerDefaultTools() {
        List.of(
            new AiTool("listNodes", "List cluster nodes."),
            new AiTool("describeTopology", "Describe cluster topology."),
            new AiTool("startWorkflow", "Start a workflow by ID."),
            new AiTool("pauseWorkflow", "Pause a workflow by ID (runtime control)."),
            new AiTool("resumeWorkflow", "Resume a workflow by ID (runtime control)."),
            new AiTool("cancelWorkflow", "Cancel a workflow by ID (runtime control)."),
            new AiTool("getWorkflowState", "Fetch workflow state by workflow ID."),
            new AiTool("assignKey", "Assign a key/partition to an app."),
            new AiTool("lock", "Acquire a distributed lock with TTL."),
            new AiTool("releaseLock", "Release a distributed lock.")
        ).forEach(aiToolRegistry::register);
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

    private ToolCallback callback(String name,
                                  String description,
                                  String inputSchema,
                                  Function<Map<String, Object>, String> executor) {
        ToolDefinition definition = DefaultToolDefinition.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema)
            .build();

        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

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

    @SuppressWarnings("unchecked")
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

    private String workflowIdSchema() {
        return """
            {"type":"object","properties":{"workflowId":{"type":"string"}},"required":["workflowId"]}
            """;
    }

    private String startWorkflowSchema() {
        return """
            {"type":"object","properties":{"workflowId":{"type":"string"},"params":{"type":"object"}},"required":["workflowId"]}
            """;
    }

    private String assignKeySchema() {
        return """
            {"type":"object","properties":{"group":{"type":"string"},"partition":{"type":"string"},"kind":{"type":"string"},"key":{"type":"string"},"appId":{"type":"string"}},"required":["group","key","appId"]}
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
}
