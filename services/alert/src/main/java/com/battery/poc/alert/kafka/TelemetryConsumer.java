package com.battery.poc.alert.kafka;

import com.battery.poc.alert.domain.AlertEvent;
import com.battery.poc.alert.domain.TelemetryEvent;
import com.battery.poc.alert.service.AlertDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryConsumer {

    private final AlertDetectionService detectionService;
    private final AlertProducer alertProducer;

    // RELIABILITY.md: DLT는 KafkaConfig.errorHandler()에서 자동 처리 (3회 실패 시)
    @KafkaListener(
        topics    = "${kafka.topics.telemetry-raw}",
        groupId   = "alert-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TelemetryEvent event) {
        log.debug("Received telemetry: batteryId={}, temp={}°C", event.getBatteryId(), event.getTemperature());

        List<AlertEvent> alerts = detectionService.detect(event);

        alerts.forEach(alert -> {
            log.warn("Alert detected: type={}, severity={}, batteryId={}",
                alert.getAlertType(), alert.getSeverity(), alert.getBatteryId());
            alertProducer.publish(alert);
        });
    }
}
