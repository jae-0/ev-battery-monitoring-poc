package com.battery.poc.alert.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

// contracts/kafka-schemas/telemetry-event.yaml 스키마와 1:1 매핑 (읽기 전용)
@Data
@NoArgsConstructor
public class TelemetryEvent {
    private String batteryId;
    private String vehicleId;
    private long   timestamp;
    private double temperature;
    private double voltage;
    private double gpsLat;
    private double gpsLng;
}
