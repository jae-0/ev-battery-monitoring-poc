package com.battery.poc.legacysync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: 1시간 주기 벌크 인서트 스케줄러 활성화 (TD-03)
@SpringBootApplication
@EnableScheduling
public class LegacySyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegacySyncApplication.class, args);
    }
}
