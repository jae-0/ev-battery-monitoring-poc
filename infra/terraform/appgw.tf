# Application Gateway with WAF v2 + AGIC (SECURITY.md)
resource "azurerm_application_gateway" "main" {
  name                = "${var.project_name}-appgw"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name

  sku {
    name     = "WAF_v2"
    tier     = "WAF_v2"
    capacity = 1  # PoC: 최소 용량. 본 사업 시 autoscale로 전환
  }

  firewall_policy_id = azurerm_web_application_firewall_policy.main.id

  gateway_ip_configuration {
    name      = "appgw-ip-config"
    subnet_id = azurerm_subnet.public[0].id
  }

  frontend_ip_configuration {
    name                 = "appgw-frontend-ip"
    public_ip_address_id = azurerm_public_ip.appgw.id
  }

  frontend_port {
    name = "http"
    port = 80
  }

  # AGIC가 관리하는 백엔드 — 더미값 (실제 라우팅은 Ingress 리소스가 제어)
  backend_address_pool {
    name = "appgw-default-pool"
  }

  backend_http_settings {
    name                  = "appgw-default-http-settings"
    cookie_based_affinity = "Disabled"
    port                  = 80
    protocol              = "Http"
    request_timeout       = 30
  }

  http_listener {
    name                           = "appgw-default-listener"
    frontend_ip_configuration_name = "appgw-frontend-ip"
    frontend_port_name             = "http"
    protocol                       = "Http"
  }

  request_routing_rule {
    name                       = "appgw-default-rule"
    rule_type                  = "Basic"
    http_listener_name         = "appgw-default-listener"
    backend_address_pool_name  = "appgw-default-pool"
    backend_http_settings_name = "appgw-default-http-settings"
    priority                   = 100
  }

  # TLS 1.2+ 강제 (AppGwSslPolicy20150501 deprecated 대응)
  ssl_policy {
    policy_type = "Predefined"
    policy_name = "AppGwSslPolicy20220101"
  }

  tags = { Name = "${var.project_name}-appgw" }

  lifecycle {
    ignore_changes = [
      # AGIC가 동적으로 수정하는 필드들 — Terraform이 덮어쓰지 않도록
      backend_address_pool,
      backend_http_settings,
      http_listener,
      probe,
      request_routing_rule,
      redirect_configuration,
      url_path_map,
      tags,
    ]
  }
}

output "appgw_public_ip" {
  description = "Application Gateway 공인 IP — 외부 API 접근 엔드포인트"
  value       = azurerm_public_ip.appgw.ip_address
}

output "appgw_name" {
  value = azurerm_application_gateway.main.name
}
