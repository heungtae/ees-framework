package com.ees.framework.control;

import com.ees.ai.control.ControlFacade;
import com.ees.ai.control.ControlFacade.ControlAssignKeyRequest;
import com.ees.ai.control.ControlFacade.ControlLockRequest;
import com.ees.ai.control.ControlFacade.ControlTopology;
import com.ees.ai.control.ControlFacade.ControlWorkflowState;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.KeyAssignment;
import com.ees.cluster.model.LockRecord;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * EES 제어(관리) API 컨트롤러.
 * <p>
 * Embedded/Standalone 모두 동일한 Control API 계약을 유지한다.
 */
@RestController
@RequestMapping("/api/control")
public class ControlController {

    private final ControlFacade facade;

    public ControlController(ControlFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/nodes")
    public Map<String, ClusterNodeRecord> nodes() {
        return facade.nodes();
    }

    @GetMapping("/topology")
    public ControlTopology topology() {
        return facade.topology();
    }

    @PostMapping("/assignments")
    public KeyAssignment assignKey(@RequestBody Map<String, Object> body) {
        String group = text(body.get("group"));
        String key = text(body.get("key"));
        String appId = text(body.get("appId"));
        String kind = text(body.get("kind"));
        int partition = intValue(body.get("partition"), 0);
        require(group, "group");
        require(key, "key");
        require(appId, "appId");
        return facade.assignKey(new ControlAssignKeyRequest(group, partition, kind, key, appId));
    }

    @PostMapping("/locks")
    public ResponseEntity<LockRecord> lock(@RequestBody Map<String, Object> body) {
        String name = text(body.get("name"));
        long ttlSeconds = longValue(body.get("ttlSeconds"), 0);
        require(name, "name");
        Optional<LockRecord> acquired = facade.lock(new ControlLockRequest(name, ttlSeconds, "http"));
        return acquired.map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(CONFLICT, "lock not acquired"));
    }

    @DeleteMapping("/locks/{name}")
    public Map<String, Object> releaseLock(@PathVariable("name") String name) {
        require(name, "name");
        boolean released = facade.releaseLock(name);
        return Map.of("released", released);
    }

    @PostMapping("/workflows/{workflowId}/start")
    public ControlWorkflowState start(@PathVariable String workflowId, @RequestBody(required = false) Map<String, Object> params) {
        require(workflowId, "workflowId");
        return facade.startWorkflow(workflowId, params == null ? Map.of() : params);
    }

    @PostMapping("/workflows/{workflowId}/pause")
    public ControlWorkflowState pause(@PathVariable String workflowId) {
        require(workflowId, "workflowId");
        return facade.pauseWorkflow(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/resume")
    public ControlWorkflowState resume(@PathVariable String workflowId) {
        require(workflowId, "workflowId");
        return facade.resumeWorkflow(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/cancel")
    public ControlWorkflowState cancel(@PathVariable String workflowId) {
        require(workflowId, "workflowId");
        return facade.cancelWorkflow(workflowId);
    }

    @GetMapping("/workflows/{workflowId}")
    public ControlWorkflowState state(@PathVariable String workflowId) {
        require(workflowId, "workflowId");
        return facade.workflowState(workflowId);
    }

    private void require(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(BAD_REQUEST, name + " is required");
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long longValue(Object value, long defaultValue) {
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
}

