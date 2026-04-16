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

  # SECURITY.md: Private 엔드포인트 (외부 접근 차단)
  delegated_subnet_id = azurerm_subnet.private[0].id
  private_dns_zone_id = azurerm_private_dns_zone.postgres.id

  depends_on = [azurerm_private_dns_zone_virtual_network_link.postgres]

  tags = { Name = "${var.project_name}-postgres" }
}

resource "azurerm_postgresql_flexible_server_database" "main" {
  name      = var.postgres_db_name
  server_id = azurerm_postgresql_flexible_server.main.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# Private DNS Zone (AKS → PostgreSQL 내부 이름 해석)
resource "azurerm_private_dns_zone" "postgres" {
  name                = "${var.project_name}.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.main.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                  = "${var.project_name}-postgres-dns-link"
  private_dns_zone_name = azurerm_private_dns_zone.postgres.name
  resource_group_name   = azurerm_resource_group.main.name
  virtual_network_id    = azurerm_virtual_network.main.id
}

# PostgreSQL 방화벽 — AKS 서브넷에서만 접근 허용 (SECURITY.md)
resource "azurerm_postgresql_flexible_server_firewall_rule" "aks" {
  name             = "allow-aks"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = "10.0.21.0"
  end_ip_address   = "10.0.21.255"
}
