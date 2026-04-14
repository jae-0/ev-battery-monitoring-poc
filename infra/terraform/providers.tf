terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  # PoC: local state. 본 사업 시 S3 backend + DynamoDB locking으로 전환
  # backend "s3" {
  #   bucket = "battery-poc-tfstate"
  #   key    = "poc/terraform.tfstate"
  #   region = "ap-northeast-2"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "battery-poc"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
