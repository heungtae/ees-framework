package com.ees.framework.example.sink;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.context.FxContext;
import com.ees.framework.sink.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI 분류 결과에 따라 알림/일반을 구분해서 수집한다.
 */
@FxSink("triage-collector")
@Component
public class TriageSink implements Sink<String> {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(TriageSink.class);

    private final List<FxContext<String>> alerts = new CopyOnWriteArrayList<>();
    private final List<FxContext<String>> normal = new CopyOnWriteArrayList<>();

    /**
     * AI 응답의 분류 결과를 읽어 ALERT 메시지는 alerts, 기타는 normal 버킷에 저장한다.
     *
     * @param context aiResponse 메타가 포함된 메시지 컨텍스트
     */
    @Override
    public void write(FxContext<String> context) {
        String classification = extractClassification(context);
        if ("ALERT".equalsIgnoreCase(classification)) {
            alerts.add(context);
            log.info("TriageSink routed ALERT message payload={}", context.message().payload());
        } else {
            normal.add(context);
            log.info("TriageSink routed NORMAL message payload={} label={}",
                context.message().payload(), classification);
        }
    }

    /**
     * ALERT로 분류된 메시지 컨텍스트를 반환한다.
     *
     * @return ALERT 목록(읽기 전용 뷰)
     */
    public List<FxContext<String>> getAlerts() {
        return List.copyOf(alerts);
    }

    /**
     * ALERT 외 메시지 컨텍스트를 반환한다.
     *
     * @return 일반 메시지 목록(읽기 전용 뷰)
     */
    public List<FxContext<String>> getNormal() {
        return List.copyOf(normal);
    }
    // extractClassification 동작을 수행한다.

    private String extractClassification(FxContext<String> context) {
        Object resp = context.meta().attributes().get("aiResponse");
        if (resp == null) {
            return "UNKNOWN";
        }
        String text = resp.toString();
        int idx = text.toUpperCase().indexOf("CLASSIFICATION:");
        if (idx >= 0) {
            String remainder = text.substring(idx + "CLASSIFICATION:".length()).trim();
            int delimiter = remainder.indexOf(';');
            return delimiter > 0 ? remainder.substring(0, delimiter).trim() : remainder.trim();
        }
        return "UNKNOWN";
    }
}
