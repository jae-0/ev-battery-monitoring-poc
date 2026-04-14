package com.battery.poc.alert.kafka;

import com.battery.poc.alert.domain.AlertEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.alert-events}")
    private String alertTopic;

    // RELIABILITY.md: Circuit Breaker — 발행 실패 시 전체 전파 차단
    @CircuitBreaker(name = "alertPublisher", fallbackMethod = "publishFallback")
    public void publish(AlertEvent event) {
        kafkaTemplate.send(alertTopic, event.getBatteryId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Alert publish failed: alertId={}", event.getAlertId(), ex);
                    throw new RuntimeException("Kafka publish failed", ex);
                }
                log.warn("ALERT published: type={}, severity={}, batteryId={}",
                    event.getAlertType(), event.getSeverity(), event.getBatteryId());
            });
    }

    public void publishFallback(AlertEvent event, Throwable t) {
        log.error("Circuit OPEN — Alert dropped: alertId={}, type={}", event.getAlertId(), event.getAlertType());
    }
}
