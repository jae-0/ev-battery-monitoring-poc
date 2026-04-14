package com.battery.poc.alert.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

// RELIABILITY.md: Circuit Breaker + Dead Letter Topic
@Configuration
public class KafkaConfig {

    private final String deadLetterTopic;

    public KafkaConfig(@org.springframework.beans.factory.annotation.Value("${kafka.topics.dead-letter}") String deadLetterTopic) {
        this.deadLetterTopic = deadLetterTopic;
    }

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // 3회 재시도 후 DLT 전송 (DAT-04 법률)
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> new org.apache.kafka.common.TopicPartition(deadLetterTopic, 0));

        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
