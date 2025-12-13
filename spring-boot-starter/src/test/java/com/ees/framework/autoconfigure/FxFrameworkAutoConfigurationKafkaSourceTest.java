package com.ees.framework.autoconfigure;

import com.ees.framework.registry.SourceRegistry;
import com.ees.framework.source.kafka.KafkaSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FxFrameworkAutoConfigurationKafkaSourceTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FxFrameworkAutoConfiguration.class))
        .withPropertyValues(
            "ees.source.kafka.enabled=true",
            "ees.source.kafka.bootstrap-servers=localhost:9092",
            "ees.source.kafka.group-id=ees",
            "ees.source.kafka.topics[0]=orders"
        );

    @Test
    void registersKafkaSourceInSourceRegistryWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(KafkaSource.class);
            SourceRegistry registry = context.getBean(SourceRegistry.class);
            assertThat(registry.getByType("kafka")).isSameAs(context.getBean(KafkaSource.class));
        });
    }
}

