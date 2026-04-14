package com.battery.poc.telemetry.repository;

import com.battery.poc.telemetry.domain.TelemetryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TelemetryRepository {

    private final DynamoDbEnhancedClient enhancedClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    // DESIGN.md: Idempotency — battery_id + timestamp 조합이 이미 존재하면 저장 안 함
    public void saveIfAbsent(TelemetryEvent event) {
        DynamoDbTable<TelemetryItem> table = enhancedClient.table(tableName, TableSchema.fromBean(TelemetryItem.class));

        Key key = Key.builder()
                .partitionValue(event.getBatteryId())
                .sortValue(event.getTimestamp())
                .build();

        if (table.getItem(key) != null) return;  // 중복 수신 무시 (Idempotency)

        table.putItem(TelemetryItem.from(event));
    }

    public Optional<TelemetryItem> findLatest(String batteryId) {
        DynamoDbTable<TelemetryItem> table = enhancedClient.table(tableName, TableSchema.fromBean(TelemetryItem.class));

        // 최신 1건 조회 (sort key 내림차순)
        var results = table.query(r -> r
                .queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
                        .keyEqualTo(Key.builder().partitionValue(batteryId).build()))
                .scanIndexForward(false)
                .limit(1));

        return results.items().stream().findFirst();
    }

    // DynamoDB 매핑 내부 클래스 (docs/generated/db-schema.md 기준)
    @DynamoDbBean
    public static class TelemetryItem {
        private String batteryId;
        private Long   timestamp;
        private String vehicleId;
        private Double temperature;
        private Double voltage;
        private Double gpsLat;
        private Double gpsLng;

        @DynamoDbPartitionKey
        @DynamoDbAttribute("battery_id")
        public String getBatteryId() { return batteryId; }
        public void setBatteryId(String v) { this.batteryId = v; }

        @DynamoDbSortKey
        @DynamoDbAttribute("timestamp")
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long v) { this.timestamp = v; }

        @DynamoDbAttribute("vehicle_id")
        public String getVehicleId() { return vehicleId; }
        public void setVehicleId(String v) { this.vehicleId = v; }

        @DynamoDbAttribute("temperature")
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double v) { this.temperature = v; }

        @DynamoDbAttribute("voltage")
        public Double getVoltage() { return voltage; }
        public void setVoltage(Double v) { this.voltage = v; }

        @DynamoDbAttribute("gps_lat")
        public Double getGpsLat() { return gpsLat; }
        public void setGpsLat(Double v) { this.gpsLat = v; }

        @DynamoDbAttribute("gps_lng")
        public Double getGpsLng() { return gpsLng; }
        public void setGpsLng(Double v) { this.gpsLng = v; }

        public static TelemetryItem from(TelemetryEvent e) {
            TelemetryItem item = new TelemetryItem();
            item.setBatteryId(e.getBatteryId());
            item.setTimestamp(e.getTimestamp());
            item.setVehicleId(e.getVehicleId());
            item.setTemperature(e.getTemperature());
            item.setVoltage(e.getVoltage());
            item.setGpsLat(e.getGpsLat());
            item.setGpsLng(e.getGpsLng());
            return item;
        }
    }
}
