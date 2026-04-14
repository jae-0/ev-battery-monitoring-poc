package com.battery.poc.legacysync.service;

import com.battery.poc.legacysync.entity.Battery;
import com.battery.poc.legacysync.repository.BatteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LegacySyncServiceTest {

    @Mock BatteryRepository batteryRepository;
    @Mock RestTemplate restTemplate;

    @InjectMocks LegacySyncService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "legacyApiBaseUrl", "http://mock-legacy-api");
    }

    private Battery battery(String id, String status) {
        Battery b = mock(Battery.class);
        given(b.getBatteryId()).willReturn(id);
        given(b.getVehicleId()).willReturn("VEH-001");
        given(b.getStatus()).willReturn(status);
        given(b.getUpdatedAt()).willReturn(LocalDateTime.now());
        return b;
    }

    @Test
    @DisplayName("변경된 배터리 없으면 레거시 API 호출 안 함")
    void sync_noChanges_doesNotCallLegacyApi() {
        given(batteryRepository.findByUpdatedAtAfter(any())).willReturn(List.of());

        service.sync();

        then(restTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("변경된 배터리 있으면 레거시 API POST 호출")
    void sync_withChanges_callsLegacyApi() {
        Battery bat = battery("BAT-001", "IN_TRANSIT");
        given(batteryRepository.findByUpdatedAtAfter(any())).willReturn(List.of(bat));
        given(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.sync();

        then(restTemplate).should().postForEntity(
                contains("/api/batteries/sync"), any(), eq(Void.class));
    }

    @Test
    @DisplayName("Idempotency — 페이로드에 syncBatchId 포함")
    void sync_payloadContainsSyncBatchId() {
        Battery bat = battery("BAT-001", "IN_TRANSIT");
        given(batteryRepository.findByUpdatedAtAfter(any())).willReturn(List.of(bat));
        given(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.sync();

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        then(restTemplate).should().postForEntity(anyString(), captor.capture(), eq(Void.class));
        assertThat(captor.getValue()).containsKey("syncBatchId");
    }

    @Test
    @DisplayName("레거시 API 성공 시 lastSyncAt 갱신")
    void sync_success_updatesLastSyncAt() {
        Battery bat = battery("BAT-001", "IN_TRANSIT");
        given(batteryRepository.findByUpdatedAtAfter(any())).willReturn(List.of(bat));
        given(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.sync();

        Map<String, Object> status = service.getStatus();
        assertThat(status.get("lastSyncResult")).isNotEqualTo("never");
        assertThat(status.get("lastSyncCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("레거시 API 실패해도 예외 전파 안 됨")
    void sync_apiFailure_doesNotThrow() {
        Battery bat = battery("BAT-001", "IN_TRANSIT");
        given(batteryRepository.findByUpdatedAtAfter(any())).willReturn(List.of(bat));
        given(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .willThrow(new RestClientException("connection refused"));

        assertThatCode(() -> service.sync()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("초기 상태 조회 시 lastSyncResult는 never")
    void getStatus_initial_returnsNever() {
        Map<String, Object> status = service.getStatus();
        assertThat(status.get("lastSyncResult")).isEqualTo("never");
        assertThat(status.get("lastSyncCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("여러 배터리 동기화 시 count 정확히 기록")
    void sync_multipleBatteries_recordsCount() {
        Battery bat1 = battery("BAT-001", "IN_TRANSIT");
        Battery bat2 = battery("BAT-002", "DELIVERED");
        Battery bat3 = battery("BAT-003", "IN_TRANSIT");
        given(batteryRepository.findByUpdatedAtAfter(any())).willReturn(List.of(bat1, bat2, bat3));
        given(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.sync();

        assertThat(service.getStatus().get("lastSyncCount")).isEqualTo(3);
    }
}
