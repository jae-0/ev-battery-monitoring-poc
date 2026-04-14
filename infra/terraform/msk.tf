resource "aws_security_group" "msk" {
  name   = "${var.project_name}-msk-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = var.private_subnet_cidrs
    description = "Kafka plaintext (내부 VPC only)"
  }

  ingress {
    from_port   = 9094
    to_port     = 9094
    protocol    = "tcp"
    cidr_blocks = var.private_subnet_cidrs
    description = "Kafka TLS (내부 VPC only)"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_msk_cluster" "main" {
  cluster_name           = "${var.project_name}-kafka"
  kafka_version          = "3.5.1"
  number_of_broker_nodes = var.msk_broker_count

  broker_node_group_info {
    instance_type   = var.msk_broker_instance_type
    client_subnets  = aws_subnet.private[*].id
    security_groups = [aws_security_group.msk.id]
    storage_info {
      ebs_storage_info { volume_size = 100 }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
      in_cluster    = true
    }
  }

  tags = { Name = "${var.project_name}-kafka" }
}

# Kafka 토픽 정의
# 실제 토픽 생성은 aws_msk_cluster 생성 후 Kafka CLI 또는 별도 provisioner로 수행
locals {
  kafka_topics = {
    telemetry_raw        = "telemetry-raw"
    telemetry_processed  = "telemetry-processed"
    alert_events         = "alert-events"
    legacy_sync_queue    = "legacy-sync-queue"
    dead_letter          = "dead-letter-topic"
  }
}
