package com.ees.ai.control;

import java.util.Map;

/**
 * Control 툴 호출 결과를 감사(audit)로 기록하는 인터페이스.
 */
public interface ControlAuditService {

    /**
     * 툴 호출 결과를 기록한다.
     *
     * @param toolName 툴 이름
     * @param args 입력 인자
     * @param result 성공 결과(실패 시 null)
     * @param error 실패 예외(성공 시 null)
     */
    void record(String toolName, Map<String, Object> args, String result, Throwable error);
}

