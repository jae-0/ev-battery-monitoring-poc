package com.battery.poc.legacysync.controller;

import com.battery.poc.legacysync.service.LegacySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 내부 운영자용 API (contracts/api-specs/legacy-sync-api.yaml)
@RestController
@RequestMapping("/v1/legacy-sync")
@RequiredArgsConstructor
public class LegacySyncController {

    private final LegacySyncService legacySyncService;

    // 수동 즉시 동기화 트리거
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> trigger() {
        legacySyncService.sync();
        return ResponseEntity.ok(Map.of("result", "triggered"));
    }

    // 마지막 동기화 상태 조회
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(legacySyncService.getStatus());
    }
}
