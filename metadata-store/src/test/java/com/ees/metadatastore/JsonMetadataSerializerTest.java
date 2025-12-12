package com.ees.metadatastore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMetadataSerializerTest {

    @Test
    void serializesAndDeserializes() {
        JsonMetadataSerializer serializer = new JsonMetadataSerializer();
        Sample value = new Sample("id-1", 42);

        byte[] bytes = serializer.serialize(value);
        Sample restored = serializer.deserialize(bytes, Sample.class);

        assertThat(restored.id()).isEqualTo("id-1");
        assertThat(restored.version()).isEqualTo(42);
    }

    private record Sample(String id, int version) {
    }
}
