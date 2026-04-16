# outputs.tf
# terraform output -json 명령으로 실제 값 확인 후 contracts/infra-outputs.yaml을 업데이트하세요.

output "aks_cluster_name" {
  description = "AKS 클러스터명 → contracts/infra-outputs.yaml: aks.cluster_name"
  value       = azurerm_kubernetes_cluster.main.name
}

output "aks_resource_group" {
  description = "AKS 리소스 그룹"
  value       = azurerm_resource_group.main.name
}

output "eventhub_bootstrap_servers" {
  description = "Event Hubs Kafka 엔드포인트 → contracts/infra-outputs.yaml: eventhub.bootstrap_servers"
  value       = "${azurerm_eventhub_namespace.main.name}.servicebus.windows.net:9093"
  sensitive   = true
}

output "eventhub_connection_string" {
  description = "Event Hubs 연결 문자열"
  value       = azurerm_eventhub_namespace_authorization_rule.app.primary_connection_string
  sensitive   = true
}

output "kafka_topic_telemetry_raw" {
  value = local.kafka_topics.telemetry_raw
}

output "kafka_topic_telemetry_processed" {
  value = local.kafka_topics.telemetry_processed
}

output "kafka_topic_alert_events" {
  value = local.kafka_topics.alert_events
}

output "kafka_topic_legacy_sync_queue" {
  value = local.kafka_topics.legacy_sync_queue
}

output "kafka_topic_dead_letter" {
  value = local.kafka_topics.dead_letter
}

output "cosmos_endpoint" {
  description = "Cosmos DB 엔드포인트 → contracts/infra-outputs.yaml: cosmosdb.endpoint"
  value       = azurerm_cosmosdb_account.main.endpoint
  sensitive   = true
}

output "cosmos_primary_key" {
  description = "Cosmos DB Primary Key"
  value       = azurerm_cosmosdb_account.main.primary_key
  sensitive   = true
}

output "cosmos_database_name" {
  value = azurerm_cosmosdb_sql_database.main.name
}

output "cosmos_container_name" {
  value = azurerm_cosmosdb_sql_container.battery_telemetry.name
}

output "postgres_fqdn" {
  description = "PostgreSQL FQDN → contracts/infra-outputs.yaml: postgres.endpoint"
  value       = azurerm_postgresql_flexible_server.main.fqdn
  sensitive   = true
}

output "acr_login_server" {
  description = "ACR 로그인 서버 주소 (이미지 Push/Pull에 사용)"
  value       = azurerm_container_registry.main.login_server
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.main.vault_uri
}

output "storage_account_name" {
  description = "Azure Blob Storage 계정명"
  value       = azurerm_storage_account.datalake.name
}
