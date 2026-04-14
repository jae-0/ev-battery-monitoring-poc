package com.battery.poc.alert.domain;

import lombok.Builder;
import lombok.Data;

// contracts/kafka-schemas/alert-event.yaml 스키마와 1:1 매핑
@Data
@Builder
public class AlertEvent {
    private String alertId;
    private String batteryId;
    private String vehicleId;
    private long   timestamp;
    private AlertType alertType;
    private Severity  severity;
    private double value;
    private double threshold;

    public enum AlertType {
        TEMPERATURE_EXCEEDED, VOLTAGE_CRITICAL, GPS_LOST
    }

    public enum Severity {
        WARNING, CRITICAL
    }
}
