package com.battery.poc.legacysync.service;

import com.battery.poc.legacysync.entity.Battery;
import com.battery.poc.legacysync.repository.BatteryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegacySyncService {

    private final BatteryRepository batteryRepository;
    private final RestTemplate restTemplate;

    @Value("${legacy.api.base-url}")
    private String legacyApiBaseUrl;

    // 마지막 동기화 시각 (메모리 보관 — PoC 범위)
    private final AtomicReference<LocalDateTime> lastSyncAt =
            new AtomicReference<>(LocalDateTime.now().minusHours(1));

    private volatile LocalDateTime lastSyncResult = null;
    private volatile int lastSyncCount = 0;

    // TD-03: 1시간 벌크 인서트 (본 사업 시 CDC로 전환)
    public void sync() {
        LocalDateTime since = lastSyncAt.get();
        List<Battery> changed = batteryRepository.findByUpdatedAtAfter(since);

        if (changed.isEmpty()) {
            log.info("No battery changes since {}", since);
            return;
        }

        String batchId = UUID.randomUUID().toString();
        log.info("Syncing {} batteries to legacy system (batchId={})", changed.size(), batchId);

        // DAT-03: Idempotency — sync_batch_id로 중복 전송 방지
        Map<String, Object> payload = new HashMap<>();
        payload.put("syncBatchId", batchId);
        payload.put("batteries", changed.stream().map(b -> Map.of(
                "batteryId", b.getBatteryId(),
                "vehicleId", b.getVehicleId(),
                "status",    b.getStatus(),
                "updatedAt", b.getUpdatedAt().toString()
        )).toList());

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    legacyApiBaseUrl + "/api/batteries/sync", payload, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                lastSyncAt.set(LocalDateTime.now());
                lastSyncResult = LocalDateTime.now();
                lastSyncCount = changed.size();
                log.info("Legacy sync completed: {} batteries (batchId={})", changed.size(), batchId);
            }
        } catch (Exception e) {
            log.error("Legacy sync failed (batchId={}): {}", batchId, e.getMessage());
            // ESC-05 해당 시 재시도 로직 또는 에스컬레이션 필요
        }
    }

    public Map<String, Object> getStatus() {
        return Map.of(
                "lastSyncAt",    lastSyncAt.get().toString(),
                "lastSyncResult", lastSyncResult != null ? lastSyncResult.toString() : "never",
                "lastSyncCount",  lastSyncCount
        );
    }
}
