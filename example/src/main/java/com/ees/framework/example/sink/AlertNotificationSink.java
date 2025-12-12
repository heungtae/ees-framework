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
 * ALERT로 분류된 메시지를 별도 버킷으로 모아 알림 전송 대상을 수집한다.
 */
@FxSink("alert-notification")
@Component
public class AlertNotificationSink implements Sink<String> {
    // logger를 반환한다.

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationSink.class);

    private final List<FxContext<String>> alerts = new CopyOnWriteArrayList<>();
    /**
     * write를 수행한다.
     * @param context 
     */

    @Override
    public void write(FxContext<String> context) {
        alerts.add(context);
        log.info("AlertNotificationSink captured ALERT payload={}", context.message().payload());
    }

    /**
     * 수집된 ALERT 메시지 컨텍스트를 반환한다.
     *
     * @return ALERT 메시지 목록(읽기 전용)
     */
    public List<FxContext<String>> getAlerts() {
        return List.copyOf(alerts);
    }
}
