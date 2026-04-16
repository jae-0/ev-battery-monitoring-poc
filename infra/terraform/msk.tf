# Azure Event Hubs (AWS MSK/Kafka 대응)
# Kafka 프로토콜 완전 호환 — 애플리케이션 코드 변경 없음
resource "azurerm_eventhub_namespace" "main" {
  name                = "${var.project_name}-eventhub"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Standard"  # Kafka 프로토콜은 Standard 이상 필요
  capacity            = var.eventhub_capacity

  # azurerm v3.x: Standard SKU는 Kafka 프로토콜 기본 지원 (kafka_enabled 제거됨)

  tags = { Name = "${var.project_name}-eventhub" }
}

# Kafka 토픽 정의 (contracts/kafka-schemas/ 기준)
locals {
  kafka_topics = {
    telemetry_raw       = "telemetry-raw"
    telemetry_processed = "telemetry-processed"
    alert_events        = "alert-events"
    legacy_sync_queue   = "legacy-sync-queue"
    dead_letter         = "dead-letter-topic"
  }
}

resource "azurerm_eventhub" "telemetry_raw" {
  name                = local.kafka_topics.telemetry_raw
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 3
  message_retention   = 1
}

resource "azurerm_eventhub" "telemetry_processed" {
  name                = local.kafka_topics.telemetry_processed
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 3
  message_retention   = 1
}

resource "azurerm_eventhub" "alert_events" {
  name                = local.kafka_topics.alert_events
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 3
  message_retention   = 1
}

resource "azurerm_eventhub" "legacy_sync_queue" {
  name                = local.kafka_topics.legacy_sync_queue
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 1
  message_retention   = 1
}

resource "azurerm_eventhub" "dead_letter" {
  name                = local.kafka_topics.dead_letter
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 1
  message_retention   = 7  # DLT는 7일 보존
}

# 서비스 연결용 공유 액세스 정책
resource "azurerm_eventhub_namespace_authorization_rule" "app" {
  name                = "app-access"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  listen              = true
  send                = true
  manage              = false
}
