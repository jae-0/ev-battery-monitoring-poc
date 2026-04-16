package com.battery.poc.telemetry.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.ThroughputProperties;
import com.azure.cosmos.models.UniqueKey;
import com.azure.cosmos.models.UniqueKeyPolicy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

// DynamoDbTableInitializer 대응 — Cosmos DB 컨테이너 초기화
@Slf4j
@Component
@RequiredArgsConstructor
public class CosmosDbInitializer {

    private final CosmosClient cosmosClient;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Value("${azure.cosmos.container}")
    private String containerName;

    @PostConstruct
    public void initializeContainer() {
        try {
            // 데이터베이스 생성 (없으면)
            cosmosClient.createDatabaseIfNotExists(databaseName);

            // DAT-03: Idempotency — batteryId + timestamp 복합 Unique Key
            UniqueKeyPolicy uniqueKeyPolicy = new UniqueKeyPolicy()
                    .setUniqueKeys(List.of(
                            new UniqueKey(List.of("/batteryId", "/timestamp"))
                    ));

            CosmosContainerProperties containerProps = new CosmosContainerProperties(containerName, "/batteryId")
                    .setUniqueKeyPolicy(uniqueKeyPolicy)
                    .setDefaultTimeToLiveInSeconds(7776000); // TTL 90일

            cosmosClient.getDatabase(databaseName)
                    .createContainerIfNotExists(containerProps);

            log.info("Cosmos DB container initialized: {}/{}", databaseName, containerName);
        } catch (Exception e) {
            log.error("Failed to initialize Cosmos DB container: {}", e.getMessage());
        }
    }
}
