package com.battery.poc.telemetry.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.battery.poc.telemetry.domain.TelemetryEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TelemetryRepository {

    private final CosmosClient cosmosClient;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Value("${azure.cosmos.container}")
    private String containerName;

    // DAT-03: Idempotency — batteryId + timestamp 중복 수신 시 무시
    public void saveIfAbsent(TelemetryEvent event) {
        String id = event.getBatteryId() + "_" + event.getTimestamp();
        CosmosContainer container = getContainer();

        try {
            container.readItem(id, new PartitionKey(event.getBatteryId()), TelemetryDocument.class);
            log.debug("Duplicate telemetry ignored: batteryId={}, timestamp={}", event.getBatteryId(), event.getTimestamp());
        } catch (Exception e) {
            // 존재하지 않으면 신규 저장
            container.createItem(TelemetryDocument.from(id, event), new PartitionKey(event.getBatteryId()), new CosmosItemRequestOptions());
        }
    }

    public Optional<TelemetryDocument> findLatest(String batteryId) {
        String query = String.format(
                "SELECT TOP 1 * FROM c WHERE c.batteryId = '%s' ORDER BY c.timestamp DESC", batteryId);

        CosmosPagedIterable<TelemetryDocument> results = getContainer()
                .queryItems(query, new CosmosQueryRequestOptions(), TelemetryDocument.class);

        return results.stream().findFirst();
    }

    private CosmosContainer getContainer() {
        return cosmosClient.getDatabase(databaseName).getContainer(containerName);
    }

    // Cosmos DB 문서 매핑 클래스 (DynamoDB TelemetryItem 대응)
    @Data
    public static class TelemetryDocument {
        @JsonProperty("id")
        private String id;

        @JsonProperty("batteryId")
        private String batteryId;

        @JsonProperty("vehicleId")
        private String vehicleId;

        @JsonProperty("timestamp")
        private Long timestamp;

        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("voltage")
        private Double voltage;

        @JsonProperty("gpsLat")
        private Double gpsLat;

        @JsonProperty("gpsLng")
        private Double gpsLng;

        public static TelemetryDocument from(String id, TelemetryEvent e) {
            TelemetryDocument doc = new TelemetryDocument();
            doc.setId(id);
            doc.setBatteryId(e.getBatteryId());
            doc.setVehicleId(e.getVehicleId());
            doc.setTimestamp(e.getTimestamp());
            doc.setTemperature(e.getTemperature());
            doc.setVoltage(e.getVoltage());
            doc.setGpsLat(e.getGpsLat());
            doc.setGpsLng(e.getGpsLng());
            return doc;
        }
    }
}
