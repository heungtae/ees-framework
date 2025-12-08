package com.ees.ai.mcp;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingMcpAuditService implements McpAuditService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMcpAuditService.class);

    @Override
    public void record(String toolName, Map<String, Object> args, String result, Throwable error) {
        if (error == null) {
            log.info("MCP tool={} args={} result={}", toolName, args, result);
        } else {
            log.warn("MCP tool={} args={} failed: {}", toolName, args, error.toString());
        }
    }
}
