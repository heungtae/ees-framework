package com.ees.ai.mcp;

import java.util.Map;

/**
 * MCP 툴 호출 결과를 감사(audit)로 기록하는 인터페이스.
 */
public interface McpAuditService {

    /**
     * 툴 호출 기록을 남긴다.
     *
     * @param toolName 툴 이름
     * @param args 툴 인자
     * @param result 성공 시 결과(실패 시 null 가능)
     * @param error 실패 시 예외(성공 시 null)
     */
    void record(String toolName, Map<String, Object> args, String result, Throwable error);
}
