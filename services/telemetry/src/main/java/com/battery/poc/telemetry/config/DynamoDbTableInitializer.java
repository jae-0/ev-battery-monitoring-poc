package com.battery.poc.telemetry.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Component
@RequiredArgsConstructor
public class DynamoDbTableInitializer {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    @PostConstruct
    public void createTableIfNotExists() {
        try {
            dynamoDbClient.describeTable(r -> r.tableName(tableName));
        } catch (ResourceNotFoundException e) {
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("battery_id").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("timestamp").attributeType(ScalarAttributeType.N).build()
                    )
                    .keySchema(
                            KeySchemaElement.builder().attributeName("battery_id").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("timestamp").keyType(KeyType.RANGE).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }
}
