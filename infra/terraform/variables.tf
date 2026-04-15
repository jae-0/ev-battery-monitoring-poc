variable "location" {
  description = "Azure 리전"
  type        = string
  default     = "koreacentral"
}

variable "environment" {
  description = "배포 환경"
  type        = string
  default     = "poc"
}

variable "project_name" {
  description = "프로젝트명 (리소스 네이밍에 사용)"
  type        = string
  default     = "glovis-poc"
}

# --- Networking ---
variable "vnet_cidr" {
  description = "VNet CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "Public Subnet CIDR 목록"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private Subnet CIDR 목록 (App/DB 배치)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

# --- AKS ---
variable "aks_node_vm_size" {
  description = "AKS 워커 노드 VM 크기"
  type        = string
  default     = "Standard_D2_v2"
}

variable "aks_node_count" {
  description = "AKS 기본 노드 수"
  type        = number
  default     = 2
}

variable "aks_node_min_count" {
  type    = number
  default = 2
}

variable "aks_node_max_count" {
  type    = number
  default = 10
}

# --- Event Hubs (Kafka) ---
variable "eventhub_capacity" {
  description = "Event Hubs 처리 단위 (TU)"
  type        = number
  default     = 2
}

# --- PostgreSQL ---
variable "postgres_sku" {
  description = "PostgreSQL Flexible Server SKU"
  type        = string
  default     = "B_Standard_B2ms"
}

variable "postgres_db_name" {
  description = "PostgreSQL 데이터베이스 이름"
  type        = string
  default     = "glovis_poc"
}

variable "postgres_admin_user" {
  description = "PostgreSQL 관리자 계정"
  type        = string
  default     = "glovis_admin"
  sensitive   = true
}

variable "postgres_admin_password" {
  description = "PostgreSQL 관리자 비밀번호 (tfvars로 주입)"
  type        = string
  sensitive   = true
}
