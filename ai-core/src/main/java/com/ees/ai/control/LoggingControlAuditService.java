package com.ees.ai.control;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J 로그로 Control 툴 호출 정보를 기록하는 {@link ControlAuditService} 구현.
 */
public class LoggingControlAuditService implements ControlAuditService {

    private static final Logger log = LoggerFactory.getLogger(LoggingControlAuditService.class);

    @Override
    public void record(String toolName, Map<String, Object> args, String result, Throwable error) {
        if (error == null) {
            log.info("Control tool={} args={} result={}", toolName, args, result);
        } else {
            log.warn("Control tool={} args={} failed: {}", toolName, args, error.toString());
        }
    }
}

