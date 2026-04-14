package com.battery.poc.legacysync.scheduler;

import com.battery.poc.legacysync.service.LegacySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacySyncScheduler {

    private final LegacySyncService legacySyncService;

    // TD-03: 1시간 주기 벌크 인서트 (application.yml의 scheduler.legacy-sync.cron)
    @Scheduled(cron = "${scheduler.legacy-sync.cron:0 0 * * * *}")
    public void scheduledSync() {
        log.info("Scheduled legacy sync started");
        legacySyncService.sync();
    }
}
