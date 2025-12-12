package com.ees.framework.example.source;

import com.ees.framework.annotations.FxSource;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.context.FxAffinity;
import com.ees.framework.source.Source;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Emits a small set of greeting messages to demonstrate a custom Source.
 */
@FxSource(type = "example-greeting")
@Component
public class GreetingSource implements Source<String> {

    private final List<String> greetings;
    private final FxCommand command;
    private final AtomicInteger sequence = new AtomicInteger();

    public GreetingSource() {
        this(List.of(
            "hello, framework",
            "welcome to the pipeline",
            "enjoy the reactive ride"
        ));
    }

    public GreetingSource(List<String> greetings) {
        this.greetings = List.copyOf(greetings);
        this.command = FxCommand.of("example-greeting");
    }

    @Override
    public Iterable<FxContext<String>> read() {
        return greetings.stream()
            .map(message -> {
                FxHeaders headers = FxHeaders.empty()
                    .with("greeting-sequence", String.valueOf(sequence.incrementAndGet()));
                FxMessage<String> fxMessage = FxMessage.now("example-greeting", message);
                FxAffinity affinity = FxAffinity.of("equipmentId", fxMessage.payload());
                return new FxContext<>(command, headers, fxMessage, FxMeta.empty(), affinity);
            })
            .toList();
    }
}
