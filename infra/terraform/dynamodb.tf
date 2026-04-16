# Azure Cosmos DB NoSQL API (AWS DynamoDB 대응)
# 무료 티어: 1,000 RU/s + 25GB 영구 무료
resource "azurerm_cosmosdb_account" "main" {
  name                = "${var.project_name}-cosmos"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  offer_type          = "Standard"
  kind                = "GlobalDocumentDB"  # NoSQL API

  # 무료 티어 활성화 (계정당 1개만 가능)
  enable_free_tier = true

  consistency_policy {
    consistency_level = "Session"
  }

  geo_location {
    location          = azurerm_resource_group.main.location
    failover_priority = 0
  }

  # SECURITY.md: at-rest 암호화 (기본 활성화, CMK는 본 사업 시 Key Vault 연동)

  # RELIABILITY.md: 자동 백업 (연속 백업 — PoC는 주기적 백업으로 비용 절감)
  backup {
    type = "Periodic"
    interval_in_minutes = 240
    retention_in_hours  = 8
  }

  tags = { Name = "${var.project_name}-cosmos" }
}

# 데이터베이스
resource "azurerm_cosmosdb_sql_database" "main" {
  name                = "battery-poc"
  resource_group_name = azurerm_resource_group.main.name
  account_name        = azurerm_cosmosdb_account.main.name
}

# 배터리 텔레메트리 컨테이너 (DynamoDB 테이블 대응)
# Partition Key: /batteryId (DynamoDB hash_key 대응)
resource "azurerm_cosmosdb_sql_container" "battery_telemetry" {
  name                = "battery-telemetry"
  resource_group_name = azurerm_resource_group.main.name
  account_name        = azurerm_cosmosdb_account.main.name
  database_name       = azurerm_cosmosdb_sql_database.main.name
  partition_key_path  = "/batteryId"

  # DAT-03: Idempotency — battery_id + timestamp 복합키 (unique key로 중복 방지)
  unique_key {
    paths = ["/batteryId", "/timestamp"]
  }

  # TTL: 90일 후 자동 삭제 (DynamoDB TTL 대응)
  default_ttl = 7776000

  indexing_policy {
    indexing_mode = "consistent"

    included_path { path = "/*" }

    excluded_path { path = "/\"_etag\"/?" }
  }
}
