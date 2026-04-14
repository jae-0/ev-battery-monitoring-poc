package com.battery.poc.alert.service;

import com.battery.poc.alert.domain.AlertEvent;
import com.battery.poc.alert.domain.AlertEvent.AlertType;
import com.battery.poc.alert.domain.AlertEvent.Severity;
import com.battery.poc.alert.domain.TelemetryEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// services/alert/CLAUDE.md 이상 징후 탐지 로직 구현
@Service
public class AlertDetectionService {

    private static final double TEMP_CRITICAL  = 60.0;
    private static final double TEMP_WARNING   = 50.0;
    private static final double VOLTAGE_MIN    = 300.0;
    private static final double VOLTAGE_MAX    = 450.0;

    public List<AlertEvent> detect(TelemetryEvent event) {
        List<AlertEvent> alerts = new ArrayList<>();

        // 온도 검사
        if (event.getTemperature() > TEMP_CRITICAL) {
            alerts.add(buildAlert(event, AlertType.TEMPERATURE_EXCEEDED, Severity.CRITICAL,
                    event.getTemperature(), TEMP_CRITICAL));
        } else if (event.getTemperature() > TEMP_WARNING) {
            alerts.add(buildAlert(event, AlertType.TEMPERATURE_EXCEEDED, Severity.WARNING,
                    event.getTemperature(), TEMP_WARNING));
        }

        // 전압 검사
        if (event.getVoltage() < VOLTAGE_MIN || event.getVoltage() > VOLTAGE_MAX) {
            double threshold = event.getVoltage() < VOLTAGE_MIN ? VOLTAGE_MIN : VOLTAGE_MAX;
            alerts.add(buildAlert(event, AlertType.VOLTAGE_CRITICAL, Severity.CRITICAL,
                    event.getVoltage(), threshold));
        }

        return alerts;
    }

    private AlertEvent buildAlert(TelemetryEvent event, AlertType type, Severity severity,
                                   double value, double threshold) {
        return AlertEvent.builder()
                .alertId(UUID.randomUUID().toString())
                .batteryId(event.getBatteryId())
                .vehicleId(event.getVehicleId())
                .timestamp(System.currentTimeMillis())
                .alertType(type)
                .severity(severity)
                .value(value)
                .threshold(threshold)
                .build();
    }
}
