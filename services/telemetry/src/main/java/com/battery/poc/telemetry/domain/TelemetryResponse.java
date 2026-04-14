package com.battery.poc.telemetry.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelemetryResponse {
    private String batteryId;
    private String vehicleId;
    private long   timestamp;
    private double temperature;
    private double voltage;
    private double gpsLat;
    private double gpsLng;
}
