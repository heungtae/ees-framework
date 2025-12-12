package com.ees.application.kafka;

import com.ees.cluster.assignment.AssignmentService;
import com.ees.cluster.kafka.KafkaConsumerAssignmentListener;
import com.ees.cluster.spring.ClusterProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample wiring that registers Kafka consumer rebalance events into the cluster AssignmentService.
 * Enable with property: sample.kafka.enabled=true
 */
@Configuration
@ConditionalOnProperty(prefix = "sample.kafka", name = "enabled", havingValue = "true")
public class KafkaClusterSampleConfiguration {
    /**
     * consumerFactory를 수행한다.
     * @param bootstrapServers 
     * @param groupId 
     * @return 
     */

    @Bean
    public ConsumerFactory<String, String> consumerFactory(
            @Value("${sample.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${sample.kafka.group-id:ees-cluster}") String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }
    /**
     * kafkaListenerContainerFactory를 수행한다.
     * @param consumerFactory 
     * @param rebalanceListener 
     * @return 
     */

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaConsumerAssignmentListener rebalanceListener) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setConsumerRebalanceListener(rebalanceListener);
        return factory;
    }
    /**
     * kafkaConsumerAssignmentListener를 수행한다.
     * @param assignmentService 
     * @param clusterProperties 
     * @param groupId 
     * @return 
     */

    @Bean
    public KafkaConsumerAssignmentListener kafkaConsumerAssignmentListener(
            AssignmentService assignmentService,
            ClusterProperties clusterProperties,
            @Value("${sample.kafka.group-id:ees-cluster}") String groupId) {
        return new KafkaConsumerAssignmentListener(assignmentService, groupId, clusterProperties.getNodeId());
    }
}
