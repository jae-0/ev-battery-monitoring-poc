package com.battery.poc.telemetry.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TelemetryRequest {

    @NotBlank
    private String batteryId;

    @NotBlank
    private String vehicleId;

    private long timestamp;   // Unix epoch milliseconds (0이면 서버 시각으로 채움)

    @NotNull
    private Double temperature;

    @NotNull
    private Double voltage;

    @NotNull
    private Double gpsLat;

    @NotNull
    private Double gpsLng;
}
