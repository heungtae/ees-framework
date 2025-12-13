package com.ees.framework.autoconfigure;

import com.ees.framework.registry.SinkRegistry;
import com.ees.framework.sink.kafka.KafkaSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FxFrameworkAutoConfigurationKafkaSinkTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FxFrameworkAutoConfiguration.class))
        .withPropertyValues(
            "ees.sink.kafka.enabled=true",
            "ees.sink.kafka.bootstrap-servers=localhost:9092",
            "ees.sink.kafka.topic=out"
        );

    @Test
    void registersKafkaSinkInSinkRegistryWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(KafkaSink.class);
            SinkRegistry registry = context.getBean(SinkRegistry.class);
            assertThat(registry.getByType("kafka")).isSameAs(context.getBean(KafkaSink.class));
        });
    }
}

