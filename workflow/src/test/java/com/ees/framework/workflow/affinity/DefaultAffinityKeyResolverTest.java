package com.ees.framework.workflow.affinity;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAffinityKeyResolverTest {

    @Test
    void usesDefaultKindWhenOnlyHeaderValueExists() {
        DefaultAffinityKeyResolver resolver = new DefaultAffinityKeyResolver("equipmentId");
        FxHeaders headers = FxHeaders.empty().with("affinity-value", "EQ-1");
        FxContext<String> context = new FxContext<>(
            FxCommand.of("cmd"),
            headers,
            FxMessage.now("src", "payload"),
            FxMeta.empty(),
            FxAffinity.none()
        );

        FxAffinity affinity = resolver.resolve(context);
        assertThat(affinity.kind()).isEqualTo("equipmentId");
        assertThat(affinity.value()).isEqualTo("EQ-1");

        resolver.setDefaultKind("lotId");
        FxAffinity updated = resolver.resolve(context);
        assertThat(updated.kind()).isEqualTo("lotId");
        assertThat(updated.value()).isEqualTo("EQ-1");
    }
}
