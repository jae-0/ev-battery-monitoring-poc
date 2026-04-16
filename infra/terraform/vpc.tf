# VNet (AWS VPC 대응)
resource "azurerm_virtual_network" "main" {
  name                = "${var.project_name}-vnet"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  address_space       = [var.vnet_cidr]

  tags = { Name = "${var.project_name}-vnet" }
}

# Public Subnet (Application Gateway/WAF만 배치 — SECURITY.md)
resource "azurerm_subnet" "public" {
  count                = length(var.public_subnet_cidrs)
  name                 = "${var.project_name}-public-${count.index + 1}"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.public_subnet_cidrs[count.index]]
}

# Private Subnet (AKS, PostgreSQL, Event Hubs 배치 — SECURITY.md)
resource "azurerm_subnet" "private" {
  count                = length(var.private_subnet_cidrs)
  name                 = "${var.project_name}-private-${count.index + 1}"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.private_subnet_cidrs[count.index]]
}

# AKS 전용 서브넷 (AKS가 별도 서브넷 권장)
resource "azurerm_subnet" "aks" {
  name                 = "${var.project_name}-aks"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.21.0/24"]
}

# NAT Gateway (Private Subnet → 외부 통신용)
resource "azurerm_public_ip" "nat" {
  name                = "${var.project_name}-nat-pip"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  allocation_method   = "Static"
  sku                 = "Standard"
}

resource "azurerm_nat_gateway" "main" {
  name                = "${var.project_name}-nat"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku_name            = "Standard"
}

resource "azurerm_nat_gateway_public_ip_association" "main" {
  nat_gateway_id       = azurerm_nat_gateway.main.id
  public_ip_address_id = azurerm_public_ip.nat.id
}

resource "azurerm_subnet_nat_gateway_association" "private" {
  count          = length(var.private_subnet_cidrs)
  subnet_id      = azurerm_subnet.private[count.index].id
  nat_gateway_id = azurerm_nat_gateway.main.id
}

resource "azurerm_subnet_nat_gateway_association" "aks" {
  subnet_id      = azurerm_subnet.aks.id
  nat_gateway_id = azurerm_nat_gateway.main.id
}

# WAF — Azure Application Gateway with WAF v2 (SECURITY.md)
resource "azurerm_public_ip" "appgw" {
  name                = "${var.project_name}-appgw-pip"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  allocation_method   = "Static"
  sku                 = "Standard"
}

resource "azurerm_web_application_firewall_policy" "main" {
  name                = "${var.project_name}-waf-policy"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name

  policy_settings {
    enabled                     = true
    mode                        = "Prevention"
    request_body_check          = true
    max_request_body_size_in_kb = 128
  }

  managed_rules {
    managed_rule_set {
      type    = "OWASP"
      version = "3.2"
    }
  }
}
