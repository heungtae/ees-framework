package com.ees.framework.example.handler;

import com.ees.framework.annotations.SinkHandlerComponent;
import com.ees.framework.context.FxContext;
import com.ees.framework.handlers.SinkHandler;
import com.ees.framework.example.sink.AlertNotificationSink;

/**
 * AI 분류 결과가 ALERT일 때 별도 알림 Sink로 전달하는 SinkHandler.
 * 기존 트리아지 Sink로의 흐름은 그대로 유지한다.
 */
@SinkHandlerComponent("alert-routing-handler")
public class AlertRoutingSinkHandler implements SinkHandler<String> {

    private static final String CLASSIFICATION_PREFIX = "CLASSIFICATION:";
    private static final String ALERT = "ALERT";

    private final AlertNotificationSink alertSink;

    public AlertRoutingSinkHandler(AlertNotificationSink alertSink) {
        this.alertSink = alertSink;
    }

    @Override
    public FxContext<String> handle(FxContext<String> context) {
        if (isAlert(context)) {
            alertSink.write(context);
        }
        return context;
    }

    @Override
    public boolean supports(FxContext<?> context) {
        return context.message() != null
            && "example-greeting".equals(context.message().sourceType());
    }

    private boolean isAlert(FxContext<String> context) {
        Object response = context.meta().attributes().get("aiResponse");
        if (response == null) {
            return false;
        }
        String text = response.toString();
        int idx = text.toUpperCase().indexOf(CLASSIFICATION_PREFIX);
        if (idx < 0) {
            return false;
        }
        String remainder = text.substring(idx + CLASSIFICATION_PREFIX.length()).trim();
        int delimiter = remainder.indexOf(';');
        String label = delimiter >= 0 ? remainder.substring(0, delimiter) : remainder;
        return ALERT.equalsIgnoreCase(label.trim());
    }
}
