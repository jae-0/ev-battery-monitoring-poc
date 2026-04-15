package com.battery.poc.telemetry.controller;

import com.battery.poc.telemetry.domain.TelemetryRequest;
import com.battery.poc.telemetry.domain.TelemetryResponse;
import com.battery.poc.telemetry.service.TelemetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TelemetryService telemetryService;

    private TelemetryRequest validRequest() {
        TelemetryRequest req = new TelemetryRequest();
        req.setBatteryId("BAT-001");
        req.setVehicleId("VEH-001");
        req.setTimestamp(1713052800000L);
        req.setTemperature(35.5);
        req.setVoltage(400.0);
        req.setGpsLat(37.5665);
        req.setGpsLng(126.9780);
        return req;
    }

    @Test
    @DisplayName("유효한 요청 → 202 Accepted")
    void ingest_validRequest_returns202() throws Exception {
        mockMvc.perform(post("/v1/telemetry")
                        .header("X-API-Key", "valid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isAccepted());

        then(telemetryService).should().ingest(any(), any());
    }

    @Test
    @DisplayName("X-API-Key 헤더 누락 → 400 Bad Request")
    void ingest_missingApiKeyHeader_returns400() throws Exception {
        mockMvc.perform(post("/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("서비스에서 401 예외 → 401 Unauthorized")
    void ingest_invalidApiKey_returns401() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key"))
                .given(telemetryService).ingest(any(), any());

        mockMvc.perform(post("/v1/telemetry")
                        .header("X-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("배터리 최신 데이터 조회 → 200 OK + 응답 바디")
    void getLatest_found_returns200() throws Exception {
        TelemetryResponse response = TelemetryResponse.builder()
                .batteryId("BAT-001").vehicleId("VEH-001")
                .timestamp(1713052800000L).temperature(35.5).voltage(400.0)
                .gpsLat(37.5665).gpsLng(126.9780).build();
        given(telemetryService.getLatest("BAT-001")).willReturn(response);

        mockMvc.perform(get("/v1/telemetry/BAT-001/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batteryId").value("BAT-001"))
                .andExpect(jsonPath("$.temperature").value(35.5));
    }

    @Test
    @DisplayName("존재하지 않는 배터리 조회 → 404 Not Found")
    void getLatest_notFound_returns404() throws Exception {
        given(telemetryService.getLatest("BAT-999"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/v1/telemetry/BAT-999/latest"))
                .andExpect(status().isNotFound());
    }
}
