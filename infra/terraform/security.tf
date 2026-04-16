data "azurerm_client_config" "current" {}

# Azure Key Vault (AWS Secrets Manager + KMS 통합 대응)
resource "azurerm_key_vault" "main" {
  name                = "${var.project_name}-kv"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "standard"

  # SECURITY.md: 삭제 보호 (PoC는 비용 절감을 위해 7일로 최소화)
  soft_delete_retention_days = 7
  purge_protection_enabled   = false  # PoC only

  # Terraform 실행 주체에게 Key Vault 접근 권한 부여
  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    secret_permissions = ["Get", "Set", "List", "Delete", "Purge"]
    key_permissions    = ["Get", "Create", "List", "Delete", "Purge", "Encrypt", "Decrypt"]
  }

  # AKS Managed Identity에게 시크릿 읽기 권한
  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = azurerm_kubernetes_cluster.main.identity[0].principal_id

    secret_permissions = ["Get", "List"]
  }

  # tfsec: azure-keyvault-specify-network-acl
  # PoC: AzureServices 허용 + 기본 Deny (본 사업 시 VNet 서비스 엔드포인트로 교체)
  network_acls {
    default_action = "Deny"
    bypass         = ["AzureServices"]
    ip_rules       = []
  }

  tags = { Name = "${var.project_name}-keyvault" }
}

# IoT API Key 시크릿 (AWS Secrets Manager iot_api_key 대응)
resource "azurerm_key_vault_secret" "iot_api_key" {
  name         = "iot-api-key"
  value        = "REPLACE_WITH_ACTUAL_KEY"
  key_vault_id = azurerm_key_vault.main.id

  lifecycle { ignore_changes = [value] }
}

# Azure Container Registry (AWS ECR 대응)
resource "azurerm_container_registry" "main" {
  name                = replace("${var.project_name}acr", "-", "")
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Basic"  # PoC: Basic 티어 (최소 비용)

  # SECURITY.md: 공개 접근 제한
  public_network_access_enabled = true  # PoC: AKS pull을 위해 허용. 본 사업 시 Private Endpoint

  admin_enabled = false  # Managed Identity로 인증 (admin 비활성화)

  tags = { Name = "${var.project_name}-acr" }
}
