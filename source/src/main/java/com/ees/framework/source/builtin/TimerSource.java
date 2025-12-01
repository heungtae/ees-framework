package com.ees.framework.source.builtin;

import com.ees.framework.annotations.FxSource;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.source.Source;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

/**
 * 주기적으로 틱 값을 생성하는 기본 Source 구현.
 */
@FxSource(type = "timer")
@Component
public class TimerSource implements Source<Long> {

    private final Duration period;
    private final FxCommand command;

    public TimerSource() {
        this(Duration.ofSeconds(1), FxCommand.of("timer"));
    }

    public TimerSource(Duration period, FxCommand command) {
        this.period = period;
        this.command = command;
    }

    @Override
    public Flux<FxContext<Long>> read() {
        return Flux.interval(period)
            .map(seq -> new FxContext<>(
                command,
                FxHeaders.empty(),
                new FxMessage<>("timer", seq, Instant.now(), null),
                FxMeta.empty()
            ));
    }
}
