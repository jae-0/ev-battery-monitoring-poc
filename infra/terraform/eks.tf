# AKS (AWS EKS 대응)
resource "azurerm_kubernetes_cluster" "main" {
  name                = "${var.project_name}-aks"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = "${var.project_name}-aks"
  kubernetes_version  = "1.29"

  default_node_pool {
    name                = "default"
    node_count          = var.aks_node_count
    vm_size             = var.aks_node_vm_size
    vnet_subnet_id      = azurerm_subnet.aks.id
    os_disk_size_gb     = 50

    # RELIABILITY.md: CPU 70% 초과 시 Auto-scaling (HPA는 K8s 레벨에서 별도 설정)
    enable_auto_scaling = true
    min_count           = var.aks_node_min_count
    max_count           = var.aks_node_max_count
  }

  # Managed Identity (AWS IAM Role 대응 — SECURITY.md 최소 권한 원칙)
  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "standard"
    outbound_type     = "userAssignedNATGateway"
  }

  # SECURITY.md: Private 엔드포인트 (외부 직접 접근 차단)
  private_cluster_enabled = false  # PoC: 관리 편의상 비활성화. 본 사업 시 true

  tags = { Name = "${var.project_name}-aks" }
}

# AKS가 ACR에서 이미지를 Pull할 수 있도록 권한 부여 (AWS ECR readonly 대응)
resource "azurerm_role_assignment" "aks_acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
  role_definition_name             = "AcrPull"
  scope                            = azurerm_container_registry.main.id
  skip_service_principal_aad_check = true
}
