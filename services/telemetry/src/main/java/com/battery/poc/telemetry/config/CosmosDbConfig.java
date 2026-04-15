package com.battery.poc.telemetry.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.ConsistencyLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosmosDbConfig {

    @Value("${azure.cosmos.uri}")
    private String cosmosUri;

    @Value("${azure.cosmos.key}")
    private String cosmosKey;

    @Bean
    public CosmosClient cosmosClient() {
        return new CosmosClientBuilder()
                .endpoint(cosmosUri)
                .key(cosmosKey)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .buildClient();
    }
}
