package com.battery.poc.telemetry.service;

import com.battery.poc.telemetry.config.ApiKeyConfig;
import com.battery.poc.telemetry.domain.TelemetryEvent;
import com.battery.poc.telemetry.domain.TelemetryRequest;
import com.battery.poc.telemetry.domain.TelemetryResponse;
import com.battery.poc.telemetry.kafka.TelemetryProducer;
import com.battery.poc.telemetry.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock ApiKeyConfig apiKeyConfig;
    @Mock TelemetryRepository repository;
    @Mock TelemetryProducer producer;

    @InjectMocks TelemetryService service;

    private TelemetryRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TelemetryRequest();
        validRequest.setBatteryId("BAT-001");
        validRequest.setVehicleId("VEH-001");
        validRequest.setTimestamp(1713052800000L);
        validRequest.setTemperature(35.5);
        validRequest.setVoltage(400.0);
        validRequest.setGpsLat(37.5665);
        validRequest.setGpsLng(126.9780);
    }

    @Test
    @DisplayName("유효한 API Key로 텔레메트리 수신 시 저장 및 Kafka 발행")
    void ingest_validApiKey_savesAndPublishes() {
        given(apiKeyConfig.isValid("valid-key")).willReturn(true);

        service.ingest(validRequest, "valid-key");

        then(repository).should().saveIfAbsent(any(TelemetryEvent.class));
        then(producer).should().publish(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("유효하지 않은 API Key → 401 예외")
    void ingest_invalidApiKey_throws401() {
        given(apiKeyConfig.isValid("wrong-key")).willReturn(false);

        assertThatThrownBy(() -> service.ingest(validRequest, "wrong-key"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("timestamp 0이면 서버 시각으로 대체")
    void ingest_zeroTimestamp_usesServerTime() {
        given(apiKeyConfig.isValid(any())).willReturn(true);
        validRequest.setTimestamp(0L);

        service.ingest(validRequest, "any-key");

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        then(repository).should().saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getTimestamp()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Idempotency — 동일 batteryId+timestamp 재전송 시 saveIfAbsent 호출")
    void ingest_duplicate_callsSaveIfAbsent() {
        given(apiKeyConfig.isValid(any())).willReturn(true);

        service.ingest(validRequest, "any-key");
        service.ingest(validRequest, "any-key");

        // saveIfAbsent 내부에서 중복 처리 — 2번 호출되지만 실제 저장은 내부 로직이 막음
        then(repository).should(times(2)).saveIfAbsent(any(TelemetryEvent.class));
    }

    @Test
    @DisplayName("저장된 배터리 최신 데이터 조회 성공")
    void getLatest_found_returnsResponse() {
        TelemetryRepository.TelemetryDocument item = new TelemetryRepository.TelemetryDocument();
        item.setBatteryId("BAT-001");
        item.setVehicleId("VEH-001");
        item.setTimestamp(1713052800000L);
        item.setTemperature(35.5);
        item.setVoltage(400.0);
        item.setGpsLat(37.5665);
        item.setGpsLng(126.9780);

        given(repository.findLatest("BAT-001")).willReturn(Optional.of(item));

        TelemetryResponse response = service.getLatest("BAT-001");

        assertThat(response.getBatteryId()).isEqualTo("BAT-001");
        assertThat(response.getTemperature()).isEqualTo(35.5);
    }

    @Test
    @DisplayName("존재하지 않는 배터리 조회 → 404 예외")
    void getLatest_notFound_throws404() {
        given(repository.findLatest("BAT-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest("BAT-999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("수신 데이터의 배터리ID, 온도, 전압이 정확히 저장됨")
    void ingest_eventFieldsMatchRequest() {
        given(apiKeyConfig.isValid(any())).willReturn(true);

        service.ingest(validRequest, "any-key");

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        then(repository).should().saveIfAbsent(captor.capture());

        TelemetryEvent event = captor.getValue();
        assertThat(event.getBatteryId()).isEqualTo("BAT-001");
        assertThat(event.getTemperature()).isEqualTo(35.5);
        assertThat(event.getVoltage()).isEqualTo(400.0);
    }
}
