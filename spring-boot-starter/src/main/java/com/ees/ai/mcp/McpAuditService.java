package com.ees.ai.mcp;

import java.util.Map;

public interface McpAuditService {

    void record(String toolName, Map<String, Object> args, String result, Throwable error);
}
