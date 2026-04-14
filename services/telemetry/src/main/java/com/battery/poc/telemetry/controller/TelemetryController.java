package com.battery.poc.telemetry.controller;

import com.battery.poc.telemetry.domain.TelemetryRequest;
import com.battery.poc.telemetry.domain.TelemetryResponse;
import com.battery.poc.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// DESIGN.md: API First — contracts/api-specs/telemetry-api.yaml 명세 기준
@RestController
@RequestMapping("/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<Void> ingest(
            @Valid @RequestBody TelemetryRequest request,
            @RequestHeader("X-API-Key") String apiKey) {
        telemetryService.ingest(request, apiKey);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{batteryId}/latest")
    public ResponseEntity<TelemetryResponse> getLatest(@PathVariable String batteryId) {
        return ResponseEntity.ok(telemetryService.getLatest(batteryId));
    }
}
