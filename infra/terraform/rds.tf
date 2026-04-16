# Azure Database for PostgreSQL Flexible Server (AWS RDS 대응)
# 12개월 무료: B1ms (1 vCore, 2GB RAM) 750시간
resource "azurerm_postgresql_flexible_server" "main" {
  name                   = "${var.project_name}-postgres"
  location               = azurerm_resource_group.main.location
  resource_group_name    = azurerm_resource_group.main.name
  version                = "15"
  administrator_login    = var.postgres_admin_user
  administrator_password = var.postgres_admin_password
  sku_name               = var.postgres_sku
  storage_mb             = 32768

  # RELIABILITY.md: 고가용성 (PoC는 비용 절감을 위해 비활성화)
  # high_availability { mode = "ZoneRedundant" }

  # RELIABILITY.md: 자동 백업 7일
  backup_retention_days        = 7
  geo_redundant_backup_enabled = false  # PoC only

  # PoC: VNet 통합 제거 (delegated_subnet + public_access 동시 사용 불가)
  # 본 사업 시: delegated_subnet_id + private_dns_zone_id 복원 후 public_network_access_enabled = false
  public_network_access_enabled = true
  zone                          = "1"  # state와 일치 (변경 시 high_availability와 교환 필요)

  tags = { Name = "${var.project_name}-postgres" }
}

resource "azurerm_postgresql_flexible_server_database" "main" {
  name      = var.postgres_db_name
  server_id = azurerm_postgresql_flexible_server.main.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# Private DNS Zone — PoC에서 VNet 통합 제거로 미사용 (본 사업 시 복원)
# resource "azurerm_private_dns_zone" "postgres" { ... }
# resource "azurerm_private_dns_zone_virtual_network_link" "postgres" { ... }
