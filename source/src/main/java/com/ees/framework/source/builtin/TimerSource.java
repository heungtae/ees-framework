package com.ees.framework.source.builtin;

import com.ees.framework.annotations.FxSource;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.source.Source;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

/**
 * 주기적으로 틱 값을 생성하는 기본 Source 구현.
 */
@FxSource(type = "timer")
@Component
public class TimerSource implements Source<Long> {

    private final Duration period;
    private final FxCommand command;
    /**
     * 인스턴스를 생성한다.
     */

    public TimerSource() {
        this(Duration.ofSeconds(1), FxCommand.of("timer"));
    }
    /**
     * 인스턴스를 생성한다.
     * @param period 
     * @param command 
     */

    public TimerSource(Duration period, FxCommand command) {
        this.period = period;
        this.command = command;
    }
    /**
     * read를 수행한다.
     * @return 
     */

    @Override
    public Iterable<FxContext<Long>> read() {
        return () -> new Iterator<>() {
            private long seq = 0;
            /**
             * hasNext를 수행한다.
             * @return 
             */

            @Override
            public boolean hasNext() {
                return true;
            }
            /**
             * next를 수행한다.
             * @return 
             */

            @Override
            public FxContext<Long> next() {
                try {
                    Thread.sleep(period.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("TimerSource interrupted", e);
                }
                return new FxContext<>(
                    command,
                    FxHeaders.empty(),
                    new FxMessage<>("timer", seq++, Instant.now(), null),
                    FxMeta.empty(),
                    com.ees.framework.context.FxAffinity.none()
                );
            }
        };
    }
}
