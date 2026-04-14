package com.battery.poc.telemetry.kafka;

import com.battery.poc.telemetry.domain.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.telemetry-raw}")
    private String topic;

    // DESIGN.md: Decoupling — 내부 서비스 간 통신은 Kafka 비동기 방식
    public void publish(TelemetryEvent event) {
        kafkaTemplate.send(topic, event.getBatteryId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka publish failed: batteryId={}", event.getBatteryId(), ex);
                } else {
                    log.debug("Published to {}: batteryId={}, offset={}",
                        topic, event.getBatteryId(), result.getRecordMetadata().offset());
                }
            });
    }
}
