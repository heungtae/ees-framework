package com.ees.framework.messaging;

/**
 * 메시징 인프라 추상화 인터페이스.
 */
public interface MessageBus {

    void publish(String topic, byte[] payload);

    void subscribe(String topic, java.util.function.Consumer<byte[]> consumer);
}
