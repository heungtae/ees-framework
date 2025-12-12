package com.ees.ai.mcp;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J 로그로 MCP 툴 호출 정보를 기록하는 {@link McpAuditService} 구현.
 */
public class LoggingMcpAuditService implements McpAuditService {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(LoggingMcpAuditService.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void record(String toolName, Map<String, Object> args, String result, Throwable error) {
        if (error == null) {
            log.info("MCP tool={} args={} result={}", toolName, args, result);
        } else {
            log.warn("MCP tool={} args={} failed: {}", toolName, args, error.toString());
        }
    }
}
