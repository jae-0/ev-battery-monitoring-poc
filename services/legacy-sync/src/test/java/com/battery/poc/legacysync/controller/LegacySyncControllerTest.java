package com.battery.poc.legacysync.controller;

import com.battery.poc.legacysync.service.LegacySyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LegacySyncController.class)
class LegacySyncControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean LegacySyncService legacySyncService;

    @Test
    @DisplayName("수동 동기화 트리거 → 200 OK, result triggered")
    void trigger_returns200() throws Exception {
        mockMvc.perform(post("/v1/legacy-sync/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("triggered"));

        then(legacySyncService).should().sync();
    }

    @Test
    @DisplayName("동기화 상태 조회 → 200 OK, 상태 정보 반환")
    void status_returns200WithSyncInfo() throws Exception {
        given(legacySyncService.getStatus()).willReturn(Map.of(
                "lastSyncAt", "2026-04-15T00:00:00",
                "lastSyncResult", "never",
                "lastSyncCount", 0));

        mockMvc.perform(get("/v1/legacy-sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastSyncCount").value(0))
                .andExpect(jsonPath("$.lastSyncResult").value("never"));
    }
}
