package com.ees.framework.example.handler;

import com.ees.framework.annotations.SourceHandlerComponent;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.handlers.SourceHandler;
import reactor.core.publisher.Mono;

/**
 * Adds basic tracing headers for greeting messages.
 */
@SourceHandlerComponent("greeting-source-handler")
public class GreetingSourceHandler implements SourceHandler<String> {

    @Override
    public Mono<FxContext<String>> handle(FxContext<String> context) {
        FxHeaders headers = context.headers()
            .with("handled-by", "GreetingSourceHandler");
        return Mono.just(new FxContext<>(context.command(), headers, context.message(), context.meta()));
    }

    @Override
    public boolean supports(FxContext<?> context) {
        return "example-greeting".equals(context.message().sourceType());
    }
}
