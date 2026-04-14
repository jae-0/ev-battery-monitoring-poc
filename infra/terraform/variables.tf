variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "배포 환경"
  type        = string
  default     = "poc"
}

variable "project_name" {
  description = "프로젝트명 (리소스 네이밍에 사용)"
  type        = string
  default     = "battery-poc"
}

# --- Networking ---
variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "Public Subnet CIDR 목록 (Multi-AZ)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private Subnet CIDR 목록 (Multi-AZ)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

variable "availability_zones" {
  description = "사용할 AZ 목록 (최소 2개)"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

# --- EKS ---
variable "eks_node_instance_type" {
  description = "EKS 워커 노드 인스턴스 타입"
  type        = string
  default     = "t3.medium"
}

variable "eks_node_desired_count" {
  type    = number
  default = 2
}

variable "eks_node_min_count" {
  type    = number
  default = 2
}

variable "eks_node_max_count" {
  type    = number
  default = 10
}

# --- MSK ---
variable "msk_broker_instance_type" {
  description = "MSK 브로커 인스턴스 타입"
  type        = string
  default     = "kafka.m5.large"
}

variable "msk_broker_count" {
  description = "MSK 브로커 수 (Multi-AZ를 위해 AZ 수의 배수)"
  type        = number
  default     = 2
}

# --- RDS ---
variable "rds_instance_class" {
  description = "RDS 인스턴스 클래스"
  type        = string
  default     = "db.t3.medium"
}

variable "rds_db_name" {
  description = "PostgreSQL 데이터베이스 이름"
  type        = string
  default     = "battery_poc"
}

variable "rds_username" {
  description = "PostgreSQL 마스터 사용자 이름"
  type        = string
  default     = "battery_admin"
  sensitive   = true
}
