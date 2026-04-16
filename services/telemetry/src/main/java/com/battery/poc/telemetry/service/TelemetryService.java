package com.battery.poc.telemetry.service;

import com.battery.poc.telemetry.config.ApiKeyConfig;
import com.battery.poc.telemetry.domain.TelemetryEvent;
import com.battery.poc.telemetry.domain.TelemetryRequest;
import com.battery.poc.telemetry.domain.TelemetryResponse;
import com.battery.poc.telemetry.kafka.TelemetryProducer;
import com.battery.poc.telemetry.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final ApiKeyConfig apiKeyConfig;
    private final TelemetryRepository repository;
    private final TelemetryProducer producer;

    public void ingest(TelemetryRequest request, String apiKey) {
        // SEC-01: API Key 검증
        if (!apiKeyConfig.isValid(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key");
        }

        long timestamp = request.getTimestamp() > 0
                ? request.getTimestamp()
                : System.currentTimeMillis();

        TelemetryEvent event = TelemetryEvent.builder()
                .batteryId(request.getBatteryId())
                .vehicleId(request.getVehicleId())
                .timestamp(timestamp)
                .temperature(request.getTemperature())
                .voltage(request.getVoltage())
                .gpsLat(request.getGpsLat())
                .gpsLng(request.getGpsLng())
                .build();

        // DAT-03: Idempotency — 중복 batteryId+timestamp 무시
        repository.saveIfAbsent(event);

        // DESIGN.md: Decoupling — Kafka 비동기 발행
        producer.publish(event);

        log.info("Telemetry ingested: batteryId={}, temp={}°C", event.getBatteryId(), event.getTemperature());
    }

    public TelemetryResponse getLatest(String batteryId) {
        return repository.findLatest(batteryId)
                .map(doc -> TelemetryResponse.builder()
                        .batteryId(doc.getBatteryId())
                        .vehicleId(doc.getVehicleId())
                        .timestamp(doc.getTimestamp())
                        .temperature(doc.getTemperature())
                        .voltage(doc.getVoltage())
                        .gpsLat(doc.getGpsLat())
                        .gpsLng(doc.getGpsLng())
                        .build())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No telemetry data for batteryId: " + batteryId));
    }
}
