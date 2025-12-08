package com.ees.framework.example.sink;

import com.ees.framework.annotations.FxSink;
import com.ees.framework.context.FxContext;
import com.ees.framework.sink.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects processed contexts for inspection and demo purposes.
 */
@FxSink("example-collector")
@Component
public class CollectingSink implements Sink<String> {

    private static final Logger log = LoggerFactory.getLogger(CollectingSink.class);

    private final List<FxContext<String>> received = new CopyOnWriteArrayList<>();

    @Override
    public Mono<Void> write(FxContext<String> context) {
        received.add(context);
        log.info("Collected context command={} payload={}", context.command(), context.message().payload());
        return Mono.empty();
    }

    public List<FxContext<String>> getReceived() {
        return List.copyOf(received);
    }
}
