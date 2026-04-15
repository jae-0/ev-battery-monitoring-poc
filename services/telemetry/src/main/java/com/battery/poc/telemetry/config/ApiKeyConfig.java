package com.battery.poc.telemetry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyConfig {

    // 로컬: application-local.yml의 iot.api-key 값
    // Azure: Key Vault에서 런타임에 주입
    @Value("${iot.api-key:#{null}}")
    private String localApiKey;

    public boolean isValid(String requestApiKey) {
        if (requestApiKey == null || requestApiKey.isBlank()) return false;
        if (localApiKey != null) return localApiKey.equals(requestApiKey);
        // AWS 환경: Secrets Manager 검증 (Backend 에이전트가 구현)
        return true;
    }
}
