# Azure Blob Storage (AWS S3 대응)
# 12개월 무료: 5GB LRS 스토리지
resource "azurerm_storage_account" "datalake" {
  name                     = replace("${var.project_name}datalake", "-", "")
  location                 = azurerm_resource_group.main.location
  resource_group_name      = azurerm_resource_group.main.name
  account_tier             = "Standard"
  account_replication_type = "LRS"  # PoC: 단일 리전. 본 사업 시 GRS

  # SECURITY.md: 공개 접근 차단
  allow_nested_items_to_be_public = false
  # PoC: Terraform이 컨테이너를 생성하려면 네트워크 접근 필요 — 본 사업 시 false + VNet 서비스 엔드포인트 구성
  public_network_access_enabled   = true

  # SECURITY.md: at-rest 암호화 (기본 활성화)
  blob_properties {
    versioning_enabled = true
  }

  tags = { Name = "${var.project_name}-datalake" }
}

# 데이터 레이크 컨테이너 (S3 버킷 하위 prefix 대응)
resource "azurerm_storage_container" "telemetry" {
  name                  = "telemetry-archive"
  storage_account_name  = azurerm_storage_account.datalake.name
  container_access_type = "private"
}

resource "azurerm_storage_container" "artifacts" {
  name                  = "artifacts"
  storage_account_name  = azurerm_storage_account.datalake.name
  container_access_type = "private"
}
