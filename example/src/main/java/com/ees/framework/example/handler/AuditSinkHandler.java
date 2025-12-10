package com.ees.framework.example.handler;

import com.ees.framework.annotations.SinkHandlerComponent;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxMeta;
import com.ees.framework.handlers.SinkHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Stamps audit metadata before data reaches the sink.
 */
@SinkHandlerComponent("audit-sink-handler")
public class AuditSinkHandler implements SinkHandler<String> {

    @Override
    public FxContext<String> handle(FxContext<String> context) {
        Map<String, Object> attributes = new HashMap<>(context.meta().attributes());
        attributes.put("auditedBy", "AuditSinkHandler");
        attributes.put("auditedAt", Instant.now().toString());

        FxMeta meta = new FxMeta(
            context.meta().sourceId(),
            context.meta().pipelineStep(),
            context.meta().retries(),
            attributes
        );
        return context.withMeta(meta);
    }

    @Override
    public boolean supports(FxContext<?> context) {
        return "example-greeting".equals(context.message().sourceType());
    }
}
